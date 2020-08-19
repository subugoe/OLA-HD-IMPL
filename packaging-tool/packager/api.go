package main

import (
	"bytes"
	"encoding/json"
	"encoding/xml"
	"fmt"
	"mime/multipart"
	"net"
	"net/http"
	"net/url"
	"sync"
	"time"

	//"github.com/go-resty/resty/v2"
	"github.com/mholt/archiver"
	"github.com/pkg/sftp"

	"io"
	"io/ioutil"
	"path/filepath"

	"compress/flate"

	"log"
	"os"
	"regexp"
	"strings"

	"github.com/beevik/etree"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/s3"

	"github.com/subugoe/bagit"

	"gopkg.in/gographics/imagick.v2/imagick"

	"github.com/go-redis/redis"
	"github.com/spf13/viper"

	"bufio"

	"golang.org/x/crypto/ssh"
	kh "golang.org/x/crypto/ssh/knownhosts"
)

var (
	appCfg     *viper.Viper
	appCfgPath string

	redisAdr    string
	redisHost   string
	redisPort   string
	redisDb     int
	redisClient *redis.Client

	s3Bucket             string
	s3FilePattern        string
	s3MetsKeyPattern     string
	s3OrigKeyPattern     string
	s3FulltextKeyPattern string

	sshUser        string
	sshPass        string
	sshPrivKeyFile string
	remoteHost     string
	remotePort     string
	knownHosts     string

	homeDir string

	fromSource string
	inPaths    []string

	skipRoot bool

	apiURL       string
	apiResource  string
	apiUser      string
	apiPasswaord string

	sourceOrganization     string
	contactEmail           string
	contactName            string
	bagitProfileIdentifier string
	bagitProfileVersion    string

	previousVersion string

	netTransp *http.Transport
	netClient *http.Client
)

func init() {

	homeDir, err := os.UserHomeDir()
	if err != nil {
		panic(err)
	}

	if len(os.Args) > 1 && os.Args[1] == "startedViaDocker" {
		appCfgPath = "/go/src/api/cfg/"
	} else {
		appCfgPath = "../cfg/"
	}

	appCfg = viper.New()
	appCfg.AddConfigPath(appCfgPath)
	appCfg.SetConfigType("env")
	appCfg.SetConfigName("app")
	if err := appCfg.ReadInConfig(); err != nil {
		if _, ok := err.(viper.ConfigFileNotFoundError); ok {
			log.Printf("Config file not found")
		} else {
			log.Printf("Config file was found but another error was produced, due to %s", err.Error())
		}
	}

	if len(os.Args) > 1 && os.Args[1] == "startedViaDocker" {
		sshPrivKeyFile = fmt.Sprintf("%s/%s", "/go/src/api/cfg", appCfg.GetString("SSH_PRIV_KEY_FILE"))
		knownHosts = fmt.Sprintf("%s/%s", "/go/src/api/cfg", appCfg.GetString("KNOWN_HOSTS"))
	} else {
		sshPrivKeyFile = fmt.Sprintf("%s/%s", homeDir, appCfg.GetString("SSH_PRIV_KEY_FILE"))
		knownHosts = fmt.Sprintf("%s/%s", homeDir, appCfg.GetString("KNOWN_HOSTS"))
	}

	//gelfWriter, err := gelf.NewWriter(graylogHost)
	//if err != nil {
	//	panic(err)
	//}
	//log.SetOutput(io.MultiWriter(os.Stderr, gelfWriter))
	log.SetOutput(io.MultiWriter(os.Stderr))

	// !!! not used in this implementation
	redisAdr = appCfg.GetString("REDIS_ADR")
	redisHost = appCfg.GetString("REDIS_HOST")
	redisPort = appCfg.GetString("REDIS_PORT")
	redisDb = appCfg.GetInt("REDIS_DB")

	redisClient = redis.NewClient(&redis.Options{
		Addr:       redisAdr,
		DB:         redisDb, // use default DB
		MaxRetries: 3,
	})

	s3Bucket = appCfg.GetString("S3_BUCKET")
	s3FilePattern = appCfg.GetString("S3_FILE_PATTERN")
	s3MetsKeyPattern = appCfg.GetString("S3_METS_KEY_PATTERN")
	s3OrigKeyPattern = appCfg.GetString("S3_ORIG_KEY_PATTERN")
	s3FulltextKeyPattern = appCfg.GetString("S3_FULLTEXT_KEY_PATTERN")

	sshUser = appCfg.GetString("SSH_USER")
	sshPass = appCfg.GetString("SSH_PASS")
	remoteHost = appCfg.GetString("REMOTE_HOST")
	remotePort = appCfg.GetString("REMOTE_PORT")

	fromSource = appCfg.GetString("FROM_SOURCE")
	inPaths = appCfg.GetStringSlice("INPATHS")

	skipRoot = appCfg.GetBool("SKIP_ROOT")

	apiURL = appCfg.GetString("API_URL")
	apiResource = appCfg.GetString("API_RESOURCE")
	apiUser = appCfg.GetString("API_USER")
	apiPasswaord = appCfg.GetString("API_PASSWAORD")

	sourceOrganization = appCfg.GetString("SOURCE_ORGANIZATION")
	contactEmail = appCfg.GetString("CONTACT_EMAIL")
	contactName = appCfg.GetString("CONTACT_NAME")
	bagitProfileIdentifier = appCfg.GetString("BAGIT_PROFILE_IDENTIFIER")
	bagitProfileVersion = appCfg.GetString("BAGIT_PROFILE_VERSION")

	previousVersion = appCfg.GetString("PREVIOUS_VERSION")

	createConn()
}

// createConn creates a HTTP Client
func createConn() {
	netTransp = &http.Transport{
		MaxIdleConns:        1024,
		MaxIdleConnsPerHost: 1024,
		TLSHandshakeTimeout: 20 * time.Second,
		Dial: (&net.Dialer{
			Timeout:   300 * time.Second,
			KeepAlive: 300 * time.Second,
		}).Dial,
	}

	netClient = &http.Client{
		//Timeout: time.Second * 600,
		Transport: netTransp,
	}
}

// getHostKey reads the public key for an given host from the knowns hosts
func getHostKey(host string) ssh.PublicKey {
	// parse OpenSSH known_hosts file
	// ssh or use ssh-keyscan to get initial key
	file, err := os.Open(filepath.Join(os.Getenv("HOME"), ".ssh", "known_hosts"))
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()

	scanner := bufio.NewScanner(file)
	var hostKey ssh.PublicKey
	for scanner.Scan() {
		fields := strings.Split(scanner.Text(), " ")
		if len(fields) != 3 {
			continue
		}

		if strings.Contains(fields[0], host) {
			var err error
			hostKey, _, _, _, err = ssh.ParseAuthorizedKey(scanner.Bytes())
			if err != nil {
				log.Fatalf("error parsing %q: %v", fields[2], err)
			}
			break
		}
	}

	if hostKey == nil {
		log.Fatalf("no hostkey found for %s", host)
	}

	return hostKey
}

func main() {

	for _, inPath := range inPaths {

		var inFileInfos []InFileInfo

		if fromSource == "local" {
			inFileInfos = listLocalPath(inPath)
		} else if fromSource == "sftp" {
			inFileInfos = listSFTPPath(inPath)
		}

		for _, inFileInfo := range inFileInfos {

			file := fmt.Sprintf(s3FilePattern, inFileInfo.Name)
			metsKey := fmt.Sprintf(s3MetsKeyPattern, inFileInfo.Name)
			origPrefix := fmt.Sprintf(s3OrigKeyPattern, inFileInfo.Name)
			fulltextPrefix := fmt.Sprintf(s3FulltextKeyPattern, inFileInfo.Name)

			client, err := getS3Client(s3Bucket)
			if err != nil {
				log.Printf("ERROR could not get S3 client, due to %s", err)
				return
			}

			err = checkExistenceInS3(client, s3Bucket, metsKey)
			if err != nil {
				log.Printf("ERROR Work doesn't exist in S3, due to %s", err)
				continue
			}
			retrieveMetsSequential(IngestInfo{
				ID:             inFileInfo.Name,
				File:           file,
				MetsKey:        metsKey,
				OrigPrefix:     origPrefix,
				FulltextPrefix: fulltextPrefix,
				Bucket:         s3Bucket,
				Context:        s3Bucket,
				Product:        s3Bucket,
				OrigPath:       inFileInfo.Path,
				OrigType:       inFileInfo.Type,
			})
		}
	}

	for {
		time.Sleep(30 * time.Second)
	}

}

// filePathWalkDir walks recursively over a local path and returns list of path
func filePathWalkDir(root string) ([]string, error) {
	var files []string

	visit := func(path string, info os.FileInfo, err error) error {
		if !info.IsDir() {
			files = append(files, path)
			log.Printf("file: %s", path)
		} else {
			log.Printf("dir: %s", path)
		}
		return nil
	}

	err := filepath.Walk(root, visit)
	if err != nil {
		log.Printf("ERROR, due to %s", err.Error())

	}
	return files, err
}

// getSSHConnection creats a SSH Client connection
func getSSHConnection() (*ssh.Client, error) {
	key, err := ioutil.ReadFile(sshPrivKeyFile)
	if err != nil {
		log.Fatalf("unable to read private key: %v", err)
	}

	// Create the Signer for this private key.
	signer, err := ssh.ParsePrivateKeyWithPassphrase(key, []byte(sshPass))
	if err != nil {
		log.Fatalf("unable to parse private key: %v", err)
	}

	hostKeyCallback, err := kh.New(knownHosts)
	if err != nil {
		log.Fatal("could not create hostkeycallback function: ", err)
	}

	config := &ssh.ClientConfig{
		User: sshUser,
		Auth: []ssh.AuthMethod{
			// Add in password check here for moar security.
			ssh.PublicKeys(signer),
			//ssh.Password(sshPass),
		},
		HostKeyCallback: hostKeyCallback,
	}
	// Connect to the remote server and perform the SSH handshake.
	return ssh.Dial("tcp", remoteHost+":"+remotePort, config)

}

// listSFTPPath returns a recursive list of a remote path
func listSFTPPath(inPath string) []InFileInfo {

	var inFileInfos []InFileInfo

	conn, err := getSSHConnection()
	if err != nil {
		log.Fatalf("ERROR It's not possible to create SSH connection, due to %s", err)
	}
	defer conn.Close()

	// open an SFTP sesison over an existing ssh connection.
	sftp, err := sftp.NewClient(conn)
	if err != nil {
		log.Fatal(err)
	}
	defer sftp.Close()

	// walk a directory
	w := sftp.Walk(inPaths[0])
	re := regexp.MustCompile("^\\S*/(\\S*).xml$")

	for w.Step() {
		if w.Err() != nil {
			continue
		}
		if w.Stat().IsDir() {
			continue
		}

		if !strings.HasSuffix(w.Path(), ".xml") {
			continue
		}

		if strings.HasSuffix(w.Path(), "result.xml") ||
			strings.HasSuffix(w.Path(), ".tei.xml") ||
			strings.HasSuffix(w.Path(), "_TEI.xml") {
			continue
		}

		match := re.FindStringSubmatch(w.Path())
		if len(match) != 2 {
			log.Printf("ERROR Path %s doesn't match pattern /^\\S*/((\\S*).xml)$/", w.Path())
			continue
		}

		if !strings.HasPrefix(match[1], "PPN") {
			continue
		}
		inf := InFileInfo{
			//File: match[1],
			Name: match[1],
			Path: w.Path(),
			Type: "sftp",
		}
		inFileInfos = append(inFileInfos, inf)

	}

	return inFileInfos
}

// listLocalPath returns a recursive list of local path
func listLocalPath(inPath string) []InFileInfo {

	var inFileInfos []InFileInfo

	paths, err := filePathWalkDir(inPath)
	if err != nil {
		log.Fatalf("ERROR Could not recursive list %s, due to %s", inPath, err.Error())
	}

	re := regexp.MustCompile("^\\S*/(\\S*).xml$")
	for _, path := range paths {

		if !strings.HasSuffix(path, ".xml") {
			continue
		}

		if strings.HasSuffix(path, "result.xml") ||
			strings.HasSuffix(path, ".tei.xml") ||
			strings.HasSuffix(path, "_TEI.xml") {
			continue
		}

		match := re.FindStringSubmatch(path)
		if len(match) != 2 {
			log.Printf("ERROR Path %s doesn't match pattern /^\\S*/((\\S*).xml)$/", path)
			continue
		}

		if !strings.HasPrefix(match[1], "PPN") {
			continue
		}
		inf := InFileInfo{
			//File: "match[1]",
			Name: "match[1]",
			Path: path,
			Type: "local",
		}
		inFileInfos = append(inFileInfos, inf)
	}

	return inFileInfos
}

// getS3Client returns a S3 client connection
func getS3Client(context string) (*s3.S3, error) {

	var accessKeyID string
	var secretAccessKey string
	var region string
	var endpoint string

	if context == "gdz" {
		accessKeyID = appCfg.GetString("S3_ACCESS_KEY_ID")
		secretAccessKey = appCfg.GetString("S3_SECRET_ACCESS_KEY")
		region = appCfg.GetString("S3_REGION")
		endpoint = appCfg.GetString("S3_ENDPOINT")
	} else {
		return nil, fmt.Errorf("ERROR S3 Context '%s' not supportet", context)
	}

	creds := credentials.NewStaticCredentials(accessKeyID, secretAccessKey, "")
	sess, err := session.NewSession(&aws.Config{
		Region:           aws.String(region),
		Credentials:      creds,
		Endpoint:         aws.String(endpoint),
		S3ForcePathStyle: aws.Bool(false),
		MaxRetries:       aws.Int(3),
	})
	if err != nil {
		return nil, fmt.Errorf("ERROR Could not create S3 session, due to %s", err)
	}

	//Create S3 service client
	return s3.New(sess), nil

}

// deriveWork removes the PPN from record identifier (for Digizeit)
func deriveWork(id string) string {
	if strings.HasPrefix(id, "PPN") {
		return id[3:]
	}
	return id
}

// checkExistenceInS3 Checks if images exists for a S3 key
func checkExistenceInS3(client *s3.S3, bucket string, key string) error {

	query := &s3.ListObjectsV2Input{
		Bucket:  aws.String(bucket),
		Prefix:  aws.String(key),
		MaxKeys: aws.Int64(int64(1)),
	}

	resp, err := client.ListObjectsV2(query)
	if err != nil {
		return fmt.Errorf("Reject processing of %s, due to %s", key, err.Error())
	}

	if len(resp.Contents) > 0 {
		return nil
	}
	return fmt.Errorf("Reject processing of %s, no images found", key)

}

// getXMLFromS3 Loads the METS from S3
func getXMLFromS3(ingestJob IngestInfo) (string, error) {

	svc, err := getS3Client(ingestJob.Context)
	if err != nil {
		return "", fmt.Errorf("ERROR could not get S3 client, due to %s", err)
	}

	result, err := svc.GetObject(&s3.GetObjectInput{
		Bucket: aws.String(ingestJob.Bucket),
		Key:    aws.String(ingestJob.MetsKey),
	})

	if err != nil {
		if strings.Contains(err.Error(), "NoSuchKey") {
			return "", fmt.Errorf("NoSuchKey %s", ingestJob.MetsKey)
		}
		return "", err

	}

	defer result.Body.Close()

	b := bytes.Buffer{}
	if _, err := io.Copy(&b, result.Body); err != nil {
		return "", err
	}

	str := fmt.Sprintf("%s", b.Bytes())

	return str, nil
}

// retrieveMetsSequential pasrse the METS and creates the ZIP (bag)
func retrieveMetsSequential(ingestJob IngestInfo) {

	log.Printf("INFO Start processinng of %s", ingestJob.ID)

	// from S3
	xmlString, err := getXMLFromS3(ingestJob)
	if err != nil {
		log.Printf("ERROR Could not (sequencial) load METS file for %s, due to %s", ingestJob.ID, err)
		return
	}
	xmlFile := strings.NewReader(xmlString)
	buf, err := ioutil.ReadAll(xmlFile)
	if err != nil {
		log.Fatalln(err)
	}

	buffer1 := bytes.NewBuffer(buf)
	buffer2 := bytes.NewBuffer(buf)
	buffer3 := bytes.NewBuffer(buf)
	buffer4 := bytes.NewBuffer(buf)
	buffer5 := bytes.NewBuffer(buf)

	defer buffer1.Reset()
	defer buffer2.Reset()
	defer buffer3.Reset()
	defer buffer4.Reset()
	defer buffer5.Reset()

	decoder1 := xml.NewDecoder(buffer1)
	decoder2 := xml.NewDecoder(buffer2)
	decoder3 := xml.NewDecoder(buffer3)

	// get RecordIdentifier
	recordIdentifier := getRecordIdentifierFromXML(decoder1, ingestJob.Context, ingestJob.MetsKey)

	// todo ask juergen to correct this
	if strings.Contains(recordIdentifier.Value, "eha_") {
		recordIdentifier.Value = strings.ReplaceAll(recordIdentifier.Value, "eha_", "")
	}

	// get valid dmdsec id's
	nonModsDmdsecIds := getNonModsDmdsecIds(decoder2, ingestJob.MetsKey)

	// parse METS in one step
	mets := getMetsStructFromXML(recordIdentifier, ingestJob, decoder3)
	mets.NonModsDmdsecIds = nonModsDmdsecIds

	// begin bag creation
	workDir, err := mkdirectories(ingestJob.ID)
	if err != nil {
		log.Fatalln(err)
	}

	// Load/Copy: 	ORIGOCR, GDZOCR, PRESENTATION and create MIN
	imageExtension, err := downloadFromS3AndReturnExtension(workDir, ingestJob, "image")
	fulltextExtension, err := downloadFromS3AndReturnExtension(workDir, ingestJob, "fulltext")
	origFulltextName, err := downloadOrigFulltextAndReturnName(workDir, ingestJob)

	// Original METS
	origMetsFile := fmt.Sprintf("%s/ORIG_%s", workDir, ingestJob.File)

	origDst, err := os.Create(origMetsFile)
	if err != nil {
		log.Fatalln(err)
	}
	defer origDst.Close()

	// Copy METS and name it <id>_orig.xml
	if _, err = io.Copy(origDst, buffer4); err != nil {
		log.Fatalln(err)
	}

	// Modify the METS	modify hrefs, add fileGrp[USE=GDZOCR]>file[ID=ORIG_OCR]
	changedMetsFile := fmt.Sprintf("%s/%s", workDir, ingestJob.File)

	doc := etree.NewDocument()

	if err := doc.ReadFromBytes(buffer5.Bytes()); err != nil {
		panic(err)
	}

	// Check inconsitency in METS, some works have fulltexts in S3, but dowsn't refer to these in METS

	size := len(doc.FindElements("//fileGrp[@USE='GDZOCR']"))
	if size == 0 {
		if fulltextExtension != "" {
			log.Printf("ERROR METS doesn't refer to fulltexts, but fulltexts exist in S3 for %s", ingestJob.ID)
		}
	}

	// Modify structMap[TYPE="LOGICAL"]

	for _, element := range doc.FindElements("//fileGrp") {
		if element.SelectAttr("USE").Value == "PRESENTATION" {
			modifyHrefs(element.FindElements("file/FLocat"), ingestJob.ID, "PRESENTATION", imageExtension)
		} else if element.SelectAttr("USE").Value == "MIN" {
			modifyHrefs(element.FindElements("file/FLocat"), ingestJob.ID, "MIN", "jpg")
			modifyMimetype(element.FindElements("file"), "image/jpg")
		} else if element.SelectAttr("USE").Value == "GDZOCR" {
			modifyHrefs(element.FindElements("file/FLocat"), ingestJob.ID, "GDZOCR", fulltextExtension)
			addOrigOcrToFulltextFilegrp(element, origFulltextName)
		} else {
			element.Parent().RemoveChildAt(element.Index())
		}
	}

	// Modify structMap[TYPE="PHYSICAL"]
	elements := doc.FindElements("//structMap[@TYPE='PHYSICAL']/div")

	addOrigOcrPhysStructmap(elements)

	str, err := doc.WriteToBytes()
	if err != nil {
		log.Printf("ERROR etree: incorrect Copy result")
	}

	_ = ioutil.WriteFile(changedMetsFile, str, 0644)

	// prepare headerfile for bagit-info.txt
	var publishers, creators, places, date, titles []string
	var rights string

	if len(mets.Dmdsecs[0].NameInfos) > 0 {
		for _, ni := range mets.Dmdsecs[0].NameInfos {

			isCreator := false

			for _, role := range ni.Roles {
				for _, term := range role.RoleTerms {
					if term.Type == "code" && term.Authority == "marcrelator" {
						if term.Value == "aut" || term.Value == "cre" {
							isCreator = true
						}
					}
				}
			}

			if !isCreator {
				continue
			}

			if len(ni.DisplayForms) > 0 {
				creators = append(creators, ni.DisplayForms[0])
			} else if len(ni.NameParts) > 0 {

				for _, np := range ni.NameParts {
					if np.Type == "" {
						creators = append(creators, np.Value)
					}
				}
			}
		}
	}

	if len(mets.Dmdsecs[0].OriginInfos) > 0 {
		for _, oi := range mets.Dmdsecs[0].OriginInfos {

			if oi.Edition == "[Electronic ed.]" {
				continue
			}

			var keyDate, notKeyDate string
			if len(oi.DateIssuedStrings) > 0 {
				for _, dateIssued := range oi.DateIssuedStrings {
					if dateIssued.KeyDate == "yes" {
						keyDate = dateIssued.Value
					} else {
						notKeyDate = dateIssued.Value
					}
				}
				if keyDate != "" {
					date = append(date, keyDate)
				} else if notKeyDate != "" {
					date = append(date, notKeyDate)
				}
			} else if len(oi.DateCreatedStrings) > 0 {
				for _, dateCreated := range oi.DateCreatedStrings {
					if dateCreated.KeyDate == "yes" {
						keyDate = dateCreated.Value
					} else {
						notKeyDate = dateCreated.Value
					}
				}
				if keyDate != "" {
					date = append(date, keyDate)
				} else if notKeyDate != "" {
					date = append(date, notKeyDate)
				}
			} else {
				continue
			}

			if len(oi.Publishers) > 0 {
				publishers = append(publishers, oi.Publishers[0])
			}
			if len(oi.Places) > 0 {
				for _, placeTerm := range oi.Places[0].PlaceTerms {
					if placeTerm.Type == "text" {
						places = append(places, placeTerm.Value)
					}
				}
			}

		}
	}

	if len(mets.Dmdsecs[0].TitleInfos) > 0 {
		for _, ti := range mets.Dmdsecs[0].TitleInfos {

			if ti.Title != "" {
				titles = append(titles, ti.Title)
			}
		}
	}

	if mets.Amdsecs[0].RightsMD.MdWrap.Rights.Owner != "" {
		rights = mets.Amdsecs[0].RightsMD.MdWrap.Rights.Owner
	}

	header := HeaderInfo{
		SourceOrganization:     sourceOrganization,
		ContactEmail:           contactEmail,
		ContactName:            contactName,
		OcrdMets:               fmt.Sprintf("data/%s.xml", ingestJob.ID),
		OcrdManifestationDepth: "partial",
		OcrdIdentifier:         ingestJob.ID,
		BagitProfileIdentifier: bagitProfileIdentifier,
		BagitProfileVersion:    bagitProfileVersion,
		Version:                "",
		DCTitle:                strings.Join(titles, ", "),
		DCCreator:              strings.Join(creators, ", "),
		DCPublisher:            strings.Join(publishers, ", "),
		DCLocation:             strings.Join(places, ", "),
		DCIssued:               strings.Join(date, ", "),
		DCRights:               rights,
	}

	headerFilePath := fmt.Sprintf("tmp/%s.json", ingestJob.ID)
	headerFile, err := os.OpenFile(headerFilePath, os.O_CREATE|os.O_WRONLY, os.ModePerm)
	if err != nil {
		log.Printf("ERROR create header file %s, due to %s", headerFilePath, err)
	}
	encoder := json.NewEncoder(headerFile)
	encoder.Encode(header)
	headerFile.Close()

	// pack the bag
	destination := packBag(ingestJob.ID, workDir)

	log.Printf("INFO Start upload to Archive for %s", ingestJob.ID)

	file, err := os.Open(destination)
	if err != nil {
		log.Printf("ERROR Could not open file %s, due to %s", destination, err)
		return
	}

	fileContents, err := ioutil.ReadAll(file)
	if err != nil {
		log.Printf("ERROR Could not read file %s, due to %s", destination, err)
		return
	}

	fi, err := file.Stat()
	if err != nil {
		log.Printf("ERROR Could not read FileInfo for %s, due to %s", destination, err)
		return
	}

	file.Close()

	body := new(bytes.Buffer)
	writer := multipart.NewWriter(body)

	part, err := writer.CreateFormFile("file", fi.Name())
	if err != nil {
		log.Printf("ERROR Could not create file attachment for %s, due to %s", destination, err)
		return
	}
	part.Write(fileContents)

	err = writer.Close()
	if err != nil {
		log.Printf("ERROR Could not close writer object, due to %s", err)
		// TODO clean up
		return
	}

	u, _ := url.ParseRequestURI(apiURL)
	u.Path = apiResource

	urlStr := fmt.Sprintf("%v", u)

	request, err := http.NewRequest("POST", urlStr, body)

	request.SetBasicAuth(apiUser, apiPasswaord)

	if err != nil {
		log.Printf("ERROR Could not create request, due to %s", err)
		return
	}

	//--header 'Authorization: Basic
	request.Header.Add("Content-Type", writer.FormDataContentType())

	//_, err = netClient.Do(request)
	resp, err := netClient.Do(request)
	log.Printf("resp: %v", resp)

	if err != nil {
		log.Printf("ERROR POST request failed, due to %s", err)
	}

	log.Printf("INFO Upload finished for %s", ingestJob.ID)

	err = os.RemoveAll(workDir)
	if err != nil {
		log.Fatalln(err)
	}

	err = os.RemoveAll(fmt.Sprintf("tmp/%s", ingestJob.ID))
	if err != nil {
		log.Fatalln(err)
	}

	err = os.RemoveAll(fmt.Sprintf("tmp/%s.json", ingestJob.ID))
	if err != nil {
		log.Fatalln(err)
	}

	err = os.RemoveAll(fmt.Sprintf("tmp/%s.zip", ingestJob.ID))
	if err != nil {
		log.Fatalln(err)
	}

}

// downloadFromS3AndReturnExtension loads the fulltexts and images from S3
func downloadFromS3AndReturnExtension(workDir string, ingestJob IngestInfo, contentType string) (string, error) {

	svc, err := getS3Client(ingestJob.Context)
	if err != nil {
		log.Printf("ERROR could not get S3 client, due to %s", err)
	}

	var prefix string
	if contentType == "fulltext" {
		prefix = ingestJob.FulltextPrefix
	} else if contentType == "image" {
		prefix = ingestJob.OrigPrefix
	}

	query := &s3.ListObjectsInput{
		Bucket: aws.String(ingestJob.Bucket),
		Prefix: aws.String(prefix),
		//MaxKeys: aws.Int64(int64(22)),
	}

	var extension string
	var b = true
	var truncatedListing = new(bool)
	var first = true

	// Flag used to check if we need to go further
	truncatedListing = &b

	for *truncatedListing {
		resp, err := svc.ListObjects(query)
		if err != nil {
			log.Printf("ERROR: %s", err.Error())
		}
		//log.Printf("len(resp.Contents): %v\n", len(resp.Contents))

		// Get all files

		if first {
			if len(resp.Contents) == 0 {
				return "", nil
			}

			if contentType == "fulltext" {
				subDir := fmt.Sprintf("%s/%s", workDir, "GDZOCR")
				err := os.MkdirAll(subDir, 0755)
				if err != nil {
					return "", fmt.Errorf("Failed to create directory %s, due to %s", subDir, err.Error())
				}

			} else if contentType == "image" {
				subDir := fmt.Sprintf("%s/%s", workDir, "PRESENTATION")
				err := os.MkdirAll(subDir, 0755)
				if err != nil {
					return "", fmt.Errorf("Failed to create directory %s, due to %s", subDir, err.Error())
				}

				subDir = fmt.Sprintf("%s/%s", workDir, "MIN")
				err = os.MkdirAll(subDir, 0755)
				if err != nil {
					return "", fmt.Errorf("Failed to create directory %s, due to %s", subDir, err.Error())
				}
			}

			first = false
		}

		extension = getAllOjects(resp, ingestJob, contentType, workDir)

		// Set continuation token

		truncatedListing = resp.IsTruncated
		if *truncatedListing {
			query.SetMarker(*resp.NextMarker)
		}
	}

	return extension, nil
}

// downloadOrigFulltextAndReturnName loads the original fulltext
func downloadOrigFulltextAndReturnName(workDir string, ingestJob IngestInfo) (string, error) {

	subDir := fmt.Sprintf("%s/%s", workDir, "ORIG_OCR")
	err := os.MkdirAll(subDir, 0755)
	if err != nil {
		return "", fmt.Errorf("Failed to create directory %s, due to %s", subDir, err.Error())
	}

	name := filepath.Base(ingestJob.OrigPath)
	path := fmt.Sprintf("%s/%s", subDir, name)

	if ingestJob.OrigType == "local" {
		localCopy(ingestJob.OrigPath, path)
	} else if ingestJob.OrigType == "sftp" {
		sftpCopy(ingestJob.OrigPath, path)
	}
	return name, nil
}

// localCopy perform a local file copy
func localCopy(src, dst string) {

	from, err := os.Open(src)
	if err != nil {
		log.Fatal(err)
	}
	defer from.Close()

	to, err := os.OpenFile(dst, os.O_RDWR|os.O_CREATE, 0666)
	if err != nil {
		log.Fatal(err)
	}
	defer to.Close()

	_, err = io.Copy(to, from)
	if err != nil {
		log.Fatal(err)
	}

}

// sftpCopy perform file copy via SFTP
func sftpCopy(src, dst string) {

	conn, err := getSSHConnection()
	if err != nil {
		log.Fatalf("unable to connect: %v", err)
	}
	defer conn.Close()

	// open an SFTP sesison over an existing ssh connection.
	sftp, err := sftp.NewClient(conn)
	if err != nil {
		log.Fatal(err)
	}
	defer sftp.Close()

	// open source file
	srcFile, err := sftp.Open(src)
	if err != nil {
		log.Fatal(err)
	}

	// create destination file
	dstFile, err := os.Create(dst)
	if err != nil {
		log.Fatal(err)
	}
	defer dstFile.Close()

	// copy source file to destination file
	// bytes, err := io.Copy(dstFile, srcFile)
	_, err = io.Copy(dstFile, srcFile)
	if err != nil {
		log.Fatal(err)
	}
	//	log.Printf("%v bytes copied", bytes)

	// flush in-memory copy
	err = dstFile.Sync()
	if err != nil {
		log.Fatal(err)
	}

}

// getAllOjects
func getAllOjects(resp *s3.ListObjectsOutput, ingestInfo IngestInfo, contentType string, workDir string) string {

	first := true
	var extension string
	toDownloadAndConvertChan := make(chan DownloadInfo, len(resp.Contents))
	var wg sync.WaitGroup

	for w := 1; w <= 12; w++ {
		go downloadImagesAndCreateDerivate(ingestInfo, toDownloadAndConvertChan, &wg)
	}

	for _, cont := range resp.Contents {

		wg.Add(1)

		i := strings.LastIndex(*cont.Key, "/")
		if string(*cont.Key)[i+1:] == "" {
			continue
		}

		re := regexp.MustCompile("^\\S*/((\\S*)\\.(\\S*))$")
		match := re.FindStringSubmatch(*cont.Key)

		if len(match) < 4 {
			continue
		}

		if first {
			extension = match[3]
			first = false
		}

		var outFilePath string

		if contentType == "fulltext" {
			outFilePath = fmt.Sprintf("%s/GDZOCR/%s", workDir, match[1])
		} else if contentType == "image" {
			outFilePath = fmt.Sprintf("%s/PRESENTATION/%s", workDir, match[1])
		}

		toDownloadAndConvertChan <- DownloadInfo{
			WorkDir:     workDir,
			OutFilePath: outFilePath,
			Key:         *cont.Key,
			ContentType: contentType,
			Page:        match[2],
		}
	}

	wg.Wait()
	close(toDownloadAndConvertChan)
	return extension

}

// downloadImagesAndCreateDerivate
func downloadImagesAndCreateDerivate(ingestInfo IngestInfo, toDownloadAndConvertChan <-chan DownloadInfo, wg *sync.WaitGroup) {

	svc, err := getS3Client(ingestInfo.Context)
	if err != nil {
		log.Printf("ERROR could not get S3 client, due to %s", err)
	}

	for downloadInfo := range toDownloadAndConvertChan {

		result, err := svc.GetObject(&s3.GetObjectInput{
			Bucket: aws.String(ingestInfo.Bucket),
			Key:    aws.String(downloadInfo.Key),
		})
		if err != nil {
			if strings.Contains(err.Error(), "NoSuchKey") {
				// todo log this
				log.Printf("ERROR NoSuchKey %s", downloadInfo.Key)
				continue
			} else {
				log.Printf("ERROR unable to load %s, due to %s", downloadInfo.Key, err)
				continue
			}
		}

		// ---

		buf, err := ioutil.ReadAll(result.Body)
		if err != nil {
			log.Fatalln(err)
		}

		buffer1 := bytes.NewBuffer(buf)
		buffer2 := bytes.NewBuffer(buf)

		// ---

		dst, err := os.Create(downloadInfo.OutFilePath)
		if err != nil {
			log.Printf("ERROR coult not create file %s, due to %s", downloadInfo.OutFilePath, err)
			result.Body.Close()
			dst.Close()
			buffer1.Reset()
			buffer2.Reset()

			continue
		}

		if _, err := io.Copy(dst, buffer1); err != nil {
			log.Printf("ERROR copy %s failed, due to %s", downloadInfo.Key, err)
			result.Body.Close()
			dst.Close()
			buffer1.Reset()
			buffer2.Reset()

			continue
		}

		if downloadInfo.ContentType == "image" {

			mw := imagick.NewMagickWand()

			err = mw.ReadImageBlob(buffer2.Bytes()) // from []byte blob
			if err != nil {
				if strings.Contains(err.Error(), "ERROR_MISSING_DELEGATE") {
					log.Printf("ERROR_MISSING_DELEGATE for %s, due to %s", downloadInfo.Key, err.Error())
				}
				log.Printf("Could't not read image data for %s, due to %s", downloadInfo.Key, err.Error())
			}

			err = mw.SetImageFormat("jpg")
			if err != nil {
				log.Printf("Could not convert image %s, due to %s", downloadInfo.Key, err.Error())
			}

			mw.SetImageResolution(300, 300)

			filter := imagick.FILTER_LANCZOS
			w := mw.GetImageWidth()
			h := mw.GetImageHeight()
			//ww := uint(300)
			//x := 100*ww/w
			//hh := h*x/100
			//err = mw.ResizeImage(ww, hh, filter)
			//err = mw.ResizeImage(w/4, h/4, filter)  // imagick.v3
			err = mw.ResizeImage(w/4, h/4, filter, 1) // imagick.v2
			if err != nil {
				//return 0, fmt.Errorf("Could not set image size to 300 x 300 for image %s, due to %s", content.inKey, err.Error())
				log.Printf("Could not set image size to 300 x 300 for image %s, due to %s", downloadInfo.Key, err.Error())
			}

			downloadInfo.OutFilePath = fmt.Sprintf("%s/MIN/%s.jpg", downloadInfo.WorkDir, downloadInfo.Page)

			dst, err := os.Create(downloadInfo.OutFilePath)
			if err != nil {
				log.Printf("ERROR coult not create file %s, due to %s", downloadInfo.OutFilePath, err)
				result.Body.Close()
				dst.Close()
				buffer1.Reset()
				buffer2.Reset()

				continue
			}

			if _, err := io.Copy(dst, bytes.NewReader(mw.GetImageBlob())); err != nil {
				log.Printf("ERROR copy %s failed, due to %s", downloadInfo.Key, err)
				result.Body.Close()
				dst.Close()
				buffer1.Reset()
				buffer2.Reset()

				continue
			}

			mw.Destroy()
		}

		result.Body.Close()
		dst.Close()
		buffer1.Reset()
		buffer2.Reset()
		wg.Done()
	}
}

// modifyHrefs ...
func modifyHrefs(elements []*etree.Element, id string, use string, extension string) {
	for _, element := range elements {

		attr := element.SelectAttr("xlink:href")

		re := regexp.MustCompile("^\\S*/((\\S*)\\.\\S*)$")
		match := re.FindStringSubmatch(attr.Value)

		if len(match) < 3 {
			log.Printf("ERROR ULR %v doesn't match pattern /^\\S*/((\\S*).\\S*)$/", attr.Value)
			continue
		} else {
			if skipRoot {
				attr.Value = fmt.Sprintf("%s/%s.%s", use, match[2], extension)
			} else {
				attr.Value = fmt.Sprintf("%s/%s/%s.%s", use, id, match[2], extension)
			}
		}
	}
}

// modifyMimetype modifies the MIMETYPE attribute
func modifyMimetype(elements []*etree.Element, mimetype string) {
	for _, element := range elements {
		attr := element.SelectAttr("MIMETYPE")
		attr.Value = "image/jpg"
	}
}

// addOrigOcrToFulltextFilegrp adds the original fulltext to the METS File section
func addOrigOcrToFulltextFilegrp(element *etree.Element, origFulltextName string) {

	e2 := &etree.Element{
		Space: "mets",
		Tag:   "FLocat",
		Attr: []etree.Attr{
			{
				Space: "",
				Key:   "LOCTYPE",
				Value: "URL",
			},
			{
				Space: "xlink",
				Key:   "href",
				Value: fmt.Sprintf("ORIG_OCR/%s", origFulltextName),
			},
		},
		Child: nil,
	}

	e1 := &etree.Element{
		Space: "mets",
		Tag:   "file",
		Attr: []etree.Attr{
			{
				Space: "",
				Key:   "ID",
				Value: "ORIG_OCR",
			},
			{
				Space: "",
				Key:   "MIMETYPE",
				Value: "text/xml",
			},
		},
		Child: make([]etree.Token, 0),
	}

	e1.AddChild(e2)
	element.AddChild(e1)
}

// addOrigOcrPhysStructmap  adds the original fulltext to the METS physical structMap
func addOrigOcrPhysStructmap(elements []*etree.Element) {

	e1 := &etree.Element{
		Space: "mets",
		Tag:   "fptr",
		Attr: []etree.Attr{
			{
				Space: "",
				Key:   "FILEID",
				Value: "ORIG_OCR",
			},
		},
		Child: make([]etree.Token, 0),
	}

	elements[0].AddChild(e1)
}

// packBag creates the bag
func packBag(id string, workDir string) string {

	b := bagit.New()

	str := ""
	outDir := fmt.Sprintf("tmp/%s", id)
	algo := "sha512"
	headerfile := fmt.Sprintf("tmp/%s.json", id)

	//vers := false
	b.SrcDir = &workDir // "Create bag. Expects path to source directory"
	b.OutDir = &outDir  // "Output directory for bag. Used with create flag"
	//tarit := false          // "Create a tar archive when creating a bag"
	//zipit := true          // "Create a tar archive when creating a bag"
	b.HashAlg = &algo         // "Hash algorithm used for manifest file when creating a bag [sha1, sha256, sha512, md5]"
	verbose := false          // "Verbose output"
	b.AddHeader = &headerfile // "Additional headers for bag-info.txt. Expects path to json file"
	b.FetchFile = &str        // "Adds optional fetch file to bag. Expects path to fetch.txt file and switch manifetch")
	b.FetchManifest = &str    // "Path to manifest file for optional fetch.txt file. Mandatory if fetch switch is used")
	b.TagManifest = &algo     // "Hash algorithm used for tag manifest file [sha1, sha256, sha512, md5]")

	if len(*b.SrcDir) != 0 {
		_, err := os.Stat(*b.SrcDir)
		if err != nil {
			log.Println("Cannot read source directory")
			return ""
		}

		_, err = os.Stat(*b.OutDir)
		if err == nil {
			log.Println("Output directory already exists. Refusing to overwrite. Quitting.")
			return ""
		}

		b.Create_without_root(verbose, skipRoot)

		z := archiver.Zip{
			CompressionLevel:       flate.DefaultCompression,
			MkdirAll:               false,
			SelectiveCompression:   true,
			ContinueOnError:        false,
			OverwriteExisting:      false,
			ImplicitTopLevelFolder: false,
		}

		files, err := ioReadDir(*b.OutDir)

		err = z.Archive(files, *b.OutDir+".zip")
		if err != nil {
			log.Println(err)
		}
	}

	//log.Printf("Finish packaging of %s", id)

	return *b.OutDir + ".zip"
}

// ioReadDir
func ioReadDir(root string) ([]string, error) {
	var files []string
	fileInfo, err := ioutil.ReadDir(root)
	if err != nil {
		return files, err
	}

	for _, file := range fileInfo {
		files = append(files, root+"/"+file.Name())
	}
	return files, nil
}

// mkdirectories
func mkdirectories(id string) (string, error) {
	workDir := fmt.Sprintf("%s", id)
	err := os.MkdirAll(workDir, 0755)
	if err != nil {
		return "", fmt.Errorf("Failed to create directory %s, due to %s", workDir, err.Error())
	}

	return workDir, err
}

// getMetsStructFromXML reads the METS data
func getMetsStructFromXML(recordIdentifier ID, ingestJob IngestInfo, decoder *xml.Decoder) *Mets {

	mets := &Mets{}

	mets.RecordIdentifier = recordIdentifier.Value
	mets.IngestJob = ingestJob

	mets.Context = ingestJob.Context
	mets.Product = ingestJob.Product
	for {
		tok, tokenErr := decoder.Token()

		if tokenErr != nil {
			if tokenErr == io.EOF {
				break
			} else {
				log.Printf("Failed to decode file, due to %s" + tokenErr.Error())
				return nil
			}
		}

		switch startElem := tok.(type) {
		case xml.StartElement:
			if startElem.Name.Local == "mets" {

				//mets := &Mets{}
				decErr := decoder.DecodeElement(mets, &startElem)
				if decErr != nil {
					log.Printf("Failed to decode file element, due to %s" + decErr.Error())
					return nil
				}
			}
		case xml.EndElement:
			//
		}

	}

	filesec := mets.Filesec
	var physStructmapExist = false
	for _, physStructmap := range mets.Structmaps {
		if physStructmap.Type == "PHYSICAL" {
			physStructmapExist = true
		}
	}

	if len(filesec.Filegrps) == 0 || !physStructmapExist {
		//mets.Doctype = "anchor"
	} else {
		mets.Doctype = "work"
	}

	mets.WorkID = recordIdentifier.Value

	return mets
}

// getNonModsDmdsecIds identifies the non-dmdsec IDs
func getNonModsDmdsecIds(decoder *xml.Decoder, key string) []string {

	var nonModsDmdsecIds []string

	dmdsec := &DmdsecCheck{}

	for {
		tok, tokenErr := decoder.Token()

		if tokenErr != nil {
			if tokenErr == io.EOF {
				break
			} else {
				log.Printf("ERROR Failed to read token in %s, due to %s", key, tokenErr.Error())
				return []string{}
			}
		}

		switch startElem := tok.(type) {
		case xml.StartElement:
			if startElem.Name.Local == "dmdSec" {

				decErr := decoder.DecodeElement(dmdsec, &startElem)
				if decErr != nil {
					log.Printf("ERROR Could not decode dmdSec in %s, due to %s", key, decErr.Error())
					continue
				}

				if strings.ToLower(dmdsec.Mdtype.Value) != "mods" {

					nonModsDmdsecIds = append(nonModsDmdsecIds, dmdsec.ID)
				}
			}
		case xml.EndElement:
			//
		}
	}

	return nonModsDmdsecIds
}

// addToIDMap
func addToIDMap(ids map[string][]ID, recordIdentifier *Recordidentifier, identifier *Identifier, IsRecordID bool) {

	if IsRecordID {
		s := strings.ReplaceAll(recordIdentifier.Source, " ", "")
		idSource := strings.ToLower(s)
		if len(ids[idSource]) == 0 {
			ids[idSource] = []ID{
				{IsRecordID,
					idSource,
					strings.TrimSpace(recordIdentifier.Value)}}
		} else {
			ids[idSource] = append(ids[idSource],
				ID{IsRecordID,
					idSource,
					strings.TrimSpace(recordIdentifier.Value)})
		}
	} else {
		t := strings.ReplaceAll(identifier.Type, " ", "")
		idType := strings.ToLower(t)
		if len(ids[idType]) == 0 {
			ids[idType] = []ID{
				{IsRecordID,
					idType,
					strings.TrimSpace(identifier.Value)}}
		} else {
			ids[idType] = append(ids[idType],
				ID{IsRecordID,
					idType,
					strings.TrimSpace(identifier.Value)})
		}
	}

}

// getRecordIdentifier identifies the Record Identifier
func getRecordIdentifier(ids map[string][]ID, ccontext string) ID {

	if ccontext == "nlh" {
		if len(ids["recordidentifier"]) != 0 {
			for _, id := range ids["recordidentifier"] {
				if id.IsRecordID {
					return id
				}
			}
		} else if len(ids["cengagegale"]) != 0 {
			for _, id := range ids["cengagegale"] {
				if id.IsRecordID {
					return id
				}
			}
		}
	} else {
		if len(ids["urn"]) != 0 {
			for _, id := range ids["urn"] {
				if id.IsRecordID {
					return id
				}
			}
		} else if len(ids["urn"]) != 0 {
			for _, id := range ids["urn"] {
				if !id.IsRecordID {
					return id
				}
			}
		} else if len(ids["gbv-ppn"]) != 0 {
			for _, id := range ids["gbv-ppn"] {
				if !id.IsRecordID {
					return id
				}
			}

		} else if len(ids["gbv-ppn"]) != 0 {
			for _, id := range ids["gbv-ppn"] {
				if id.IsRecordID {
					return id
				}
			}

		} else if len(ids["recordidentifier"]) != 0 {
			for _, id := range ids["recordidentifier"] {
				if id.IsRecordID {
					return id
				}
			}
		} else if len(ids["swb-ppn"]) != 0 {
			for _, id := range ids["swb-ppn"] {
				if id.IsRecordID {
					return id
				}
			}
		} else if len(ids["spo-id"]) != 0 {
			for _, id := range ids["spo-id"] {
				if id.IsRecordID {
					return id
				}
			}
		} else if len(ids["zdb-id"]) != 0 {
			for _, id := range ids["zdb-id"] {
				if id.IsRecordID {
					return id
				}
			}
		} else if len(ids["oai"]) != 0 {
			for _, id := range ids["oai"] {
				if !id.IsRecordID {
					return id
				}
			}
		} else if len(ids["de-611"]) != 0 {
			for _, id := range ids["de-611"] {
				if id.IsRecordID {
					return id
				}
			}
		}

	}
	return ID{}
}

// getRecordIdentifierFromXML
func getRecordIdentifierFromXML(decoder *xml.Decoder, ccontext string, key string) ID {

	//var i int
	var isRelatedItem bool
	var firstDmdsec bool
	var ids map[string][]ID

	for {
		tok, tokenErr := decoder.Token()

		if tokenErr != nil {
			if tokenErr == io.EOF {
				break
			} else {
				log.Printf("ERROR Failed to read token in %s, due to %s", key, tokenErr.Error())
				return ID{}
			}
		}

		switch startElem := tok.(type) {
		case xml.StartElement:
			if isRelatedItem {
				continue
			}

			if startElem.Name.Local == "relatedItem" {
				isRelatedItem = true
			} else if startElem.Name.Local == "dmdSec" {

				firstDmdsec = true

				//i = 0
				ids = make(map[string][]ID)
			} else if startElem.Name.Local == "identifier" {
				identifier := &Identifier{}
				decErr := decoder.DecodeElement(identifier, &startElem)
				if decErr != nil {
					log.Printf("ERROR Could not decode identifier in %s, due to %s", key, decErr.Error())
					continue
				}

				addToIDMap(ids, nil, identifier, false)
				//i += 1

			} else if startElem.Name.Local == "recordIdentifier" {
				recordIdentifier := &Recordidentifier{}
				decErr := decoder.DecodeElement(recordIdentifier, &startElem)
				if decErr != nil {
					log.Printf("ERROR Could not decode recordidentifier in %s, due to %s", key, decErr.Error())
					continue
				}

				// add dummy source, if nil
				if recordIdentifier.Source == "" {
					recordIdentifier.Source = "recordidentifier"
				}

				addToIDMap(ids, recordIdentifier, nil, true)

				//i += 1
			}
		case xml.EndElement:
			if startElem.Name.Local == "relatedItem" {
				isRelatedItem = false
			} else if startElem.Name.Local == "dmdSec" {

				if firstDmdsec == true {
					rid := getRecordIdentifier(ids, ccontext)

					return rid
				}

			}
		}
	}
	return ID{}
}

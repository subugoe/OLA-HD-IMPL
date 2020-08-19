package main

import (
	"encoding/xml"
)

// AuthSuccess ...
type AuthSuccess struct {
	HTTPCode   int    `json:"httpCode,omitempty"`
	HTTPStatus string `json:"httpStatus,omitempty"`
	Message    string `json:"message,omitempty"`
	Path       string `json:"path,omitempty"`
	Pid        string `json:"pid,omitempty"`
	Timestamp  string `json:"timestamp,omitempty"`

	AccessToken string `json:"accessToken,omitempty"`
	ExpiredTime int32  `json:"expiredTime,omitempty"`
	TokenType   string `json:"tokenType,omitempty"`
}

// DownloadInfo ...
type DownloadInfo struct {
	WorkDir     string
	OutFilePath string
	Key         string
	ContentType string
	Page        string
}

// HeaderInfo ...
type HeaderInfo struct {
	SourceOrganization     string `json:"Source-Organization"`
	ContactEmail           string `json:"Contact-Email"`
	ContactName            string `json:"Contact-Name,omitempty"`
	OcrdIdentifier         string `json:"Ocrd-Identifier"`
	OcrdMets               string `json:"Ocrd-Mets"`
	OcrdManifestationDepth string `json:"Ocrd-Manifestation-Depth"`
	BagitProfileIdentifier string `json:"BagIt-Profile-Identifier,omitempty"`
	BagitProfileVersion    string `json:"BagIt-Profile-Version,omitempty"`
	Version                string `json:"Version,omitempty"`
	DCIdentifier           string `json:"DC.identifier,omitempty"`
	DCTitle                string `json:"DC.title,omitempty"`
	DCCreator              string `json:"DC.creator,omitempty"`
	DCPublisher            string `json:"DC.publisher,omitempty"`
	DCLocation             string `json:"DC.location,omitempty"`
	DCIssued               string `json:"DC.issued,omitempty"`
	DCRights               string `json:"DC.rights,omitempty"`
	DCLicense              string `json:"DC.license,omitempty"`
}

// InFileInfo ...
type InFileInfo struct {
	Name string
	//File string
	Path string
	Type string // {sftp || local}
}

// IngestInfo ...
type IngestInfo struct {
	ID             string `json:"id"`
	File           string `json:"file"`
	MetsKey        string `json:"mets_key"`
	OrigPrefix     string `json:"orig_prefix"`
	FulltextPrefix string `json:"fulltext_prefix"`
	Bucket         string `json:"bucket"`
	Context        string `json:"context"`
	Product        string `json:"product"`
	OrigPath       string `json:"orig_path"`
	OrigType       string `json:"orig_type"` // {sftp || local}
}

// ID ...
type ID struct {
	IsRecordID bool
	Type       string
	Value      string
	//Invalid    string
}

// Identifier ...
type Identifier struct {
	// type and value: ,omitempty?
	Type    string `xml:"type,attr" json:"type,omitempty" bson:"type,omitempty"`
	Value   string `xml:",chardata" json:"id,omitempty" bson:"id,omitempty"`
	Invalid string `xml:"invalid,attr" json:"-" bson:"-"`
}

// Recordidentifier ...
type Recordidentifier struct {
	Source string `xml:"source,attr"`
	Value  string `xml:",chardata"`
}

// Mets ...
type Mets struct {
	Context          string
	Product          string
	RecordIdentifier string
	WorkID           string     // helper
	IngestJob        IngestInfo // helper
	Doctype          string
	XMLName          xml.Name    `xml:"mets"`
	Dmdsecs          []Dmdsec    `xml:"dmdSec"`
	Amdsecs          []Amdsec    `xml:"amdSec"`     // required, repeatable
	Filesec          Filesec     `xml:"fileSec"`    // (required), not repeatable
	Structmaps       []Structmap `xml:"structMap"`  // required, repeatable (for TYPE: logical), not repeatable for physical structMap
	Structlink       Structlink  `xml:"structLink"` // (required), not repeatable
	NonModsDmdsecIds []string    // helper
}

// Structlink ...
type Structlink struct {
	XMLName xml.Name `xml:"structLink"`
	//Title        string   `xml:"title"`
	ID     string   `xml:"ID,attr"`
	Smlink []Smlink `xml:"smLink"` // required, repeatable
}

// Smlink ...
type Smlink struct {
	XMLName xml.Name `xml:"smLink"`
	From    string   `xml:"from,attr"`
	To      string   `xml:"to,attr"`
}

// Structmap ...
type Structmap struct {
	XMLName xml.Name `xml:"structMap"`
	Type    string   `xml:"TYPE,attr"`
	Divs    []Div    `xml:"div"` // required, repeatable
}

// Div ...
type Div struct {
	XMLName                 xml.Name `xml:"div"`
	_Order                  int64    // helper
	IsPartOfMultivolumework bool     // helper

	// logical and physical
	ID    string `xml:"ID,attr"`
	Type  string `xml:"TYPE,attr"`
	DmdID string `xml:"DMDID,attr"`
	Divs  []Div  `xml:"div"` // required, repeatable

	ParentID   string // helper
	ParentWork string // helper
	ParentLog  string // helper
	WorkID     string // helper
	//Structrun  []Structrun // helper

	// physical tags
	Order      string `xml:"ORDER,attr"`      // required
	Orderlabel string `xml:"ORDERLABEL,attr"` // optional
	ContentID  string `xml:"CONTENTIDS,attr"` // optionnal, identifizierende PURL und/oder URN mit Leerzeichen getrennt
	Fptr       []Fptr `xml:"fptr"`            // required, repeatable

	// logical tags
	Label string `xml:"LABEL,attr"`
	AdmID string `xml:"ADMID,attr"`
	Mptr  Mptr   `xml:"mptr"` // optional, not repeatable

	DivPosition int64 // helper
	Level       int8  // helper
}

// Fptr ...
type Fptr struct {
	// nicht teil des DFG-Viewer Anwend.prof.
	XMLName xml.Name `xml:"fptr"`
	Fileid  string   `xml:"FILEID,attr"`
}

// Mptr ...
type Mptr struct {
	XMLName xml.Name `xml:"mptr"`
	Loctype string   `xml:"LOCTYPE,attr"`
	Href    string   `xml:"href,attr"`
}

// Filesec ...
type Filesec struct {
	XMLName  xml.Name  `xml:"fileSec"`
	Filegrps []Filegrp `xml:"fileGrp"` // required, repeatable
}

// Filegrp ...
type Filegrp struct {
	XMLName xml.Name `xml:"fileGrp"`
	Use     string   `xml:"USE,attr"`
	Files   []File   `xml:"file"` // required, repeatable
}

// File ...
type File struct {
	XMLName  xml.Name `xml:"file"`
	ID       string   `xml:"ID,attr"`
	Mimetype string   `xml:"MIMETYPE,attr"`
	FLocat   FLocat   `xml:"FLocat"` // required, not repeatable
}

// FLocat ...
type FLocat struct {
	XMLName xml.Name `xml:"FLocat"`
	Loctype string   `xml:"LOCTYPE,attr"`
	Href    string   `xml:"href,attr"`
}

// Amdsec ...
type Amdsec struct {
	XMLName    xml.Name   `xml:"amdSec"`
	ID         string     `xml:"ID,attr"`
	RightsMD   RightsMD   `xml:"rightsMD"`   // required, not repeatable
	DigiprovMD DigiprovMD `xml:"digiprovMD"` // required, not repeatable
	//TechMD  TechMD   `xml:"techMD"` // optional, not repeatable
}

// RightsMD ...
type RightsMD struct {
	ID     string       `xml:"ID,attr"`
	MdWrap RightsMdWrap `xml:"mdWrap"` // required, not repeatable
}

// RightsMdWrap ...
type RightsMdWrap struct {
	MDTYPE      string `xml:"MDTYPE,attr"`
	OTHERMDTYPE string `xml:"OTHERMDTYPE,attr"`
	Rights      Rights `xml:"xmlData>rights"` // required, not repeatable
	Links       Links  `xml:"xmlData>links"`  // required, not repeatable
}

// Rights ...
type Rights struct {
	Owner             string `xml:"owner" json:"rights_owner,omitempty" bson:"rights_owner,omitempty"`                                // required, not repeatable
	OwnerLogo         string `xml:"ownerLogo" json:"rights_owner_logo,omitempty" bson:"rights_owner_logo,omitempty"`                  // required, not repeatable
	OwnerSiteURL      string `xml:"ownerSiteURL" json:"rights_owner_site_url,omitempty" bson:"rights_owner_site_url,omitempty"`       // required, not repeatable
	OwnerContact      string `xml:"ownerContact" json:"rights_owner_contact,omitempty" bson:"rights_owner_contact,omitempty"`         // required, not repeatable
	Aggregator        string `xml:"aggregator" json:"aggregator,omitempty" bson:"aggregator,omitempty"`                               // optional, not repeatable
	AggregatorLogo    string `xml:"aggregatorLogo" json:"aggregator_logo,omitempty" bson:"aggregator_logo,omitempty"`                 // optional, not repeatable
	AggregatorSiteURL string `xml:"aggregatorSiteURL" json:"aggregator_site_url,omitempty" bson:"aggregator_site_url,omitempty"`      // optional, not repeatable
	Sponsor           string `xml:"sponsor" json:"rights_sponsor,omitempty" bson:"rights_sponsor,omitempty"`                          // optional, not repeatable
	SponsorLogo       string `xml:"sponsorLogo" json:"rights_sponsor_logo,omitempty" bson:"rights_sponsor_logo,omitempty"`            // optional, not repeatable
	SponsorSiteURL    string `xml:"sponsorSiteURL" json:"rights_sponsor_site_url,omitempty" bson:"rights_sponsor_site_url,omitempty"` // optional, not repeatable
	License           string `xml:"license" json:"rights_license,omitempty" bson:"rights_license,omitempty"`                          // optional, not repeatable
	//Reference           string `xml:"reference" json:"rights_reference,omitempty" bson:"rights_reference,omitempty"`                          // optional, not repeatable
}

// DigiprovMD ...
type DigiprovMD struct {
	ID     string         `xml:"ID,attr"`
	MdWrap DigiprovMdWrap `xml:"mdWrap"` // required, not repeatable
}

// DigiprovMdWrap ...
type DigiprovMdWrap struct {
	MDTYPE      string `xml:"MDTYPE,attr"`
	OTHERMDTYPE string `xml:"OTHERMDTYPE,attr"`
	Links       Links  `xml:"xmlData>links"` // required, not repeatable
}

// Links ...
type Links struct {
	Reference    []Reference `xml:"reference"`    // required, repeatable
	Presentation string      `xml:"presentation"` // optional, not repeatable
	Sru          string      `xml:"sru"`          // optional, not repeatable
	Iiif         string      `xml:"iiif"`         // optional, not repeatable
}

// Reference ...
type Reference struct {
	Linktext string `xml:"linktext,attr"`
	Value    string `xml:",chardata"`
}

// DmdsecCheck ..
type DmdsecCheck struct {
	XMLName xml.Name `xml:"dmdSec"`
	ID      string   `xml:"ID,attr"`
	Mdtype  MdWrap   `xml:"mdWrap"`
}

// MdWrap ...
type MdWrap struct {
	Value string `xml:"MDTYPE,attr"`
}

// Dmdsec ...
type Dmdsec struct {
	XMLName     xml.Name     `xml:"dmdSec"`
	ID          string       `xml:"ID,attr"`
	Identifiers []Identifier `xml:"mdWrap>xmlData>mods>identifier"`
	TitleInfos  []TitleInfo  `xml:"mdWrap>xmlData>mods>titleInfo" json:"title"` // required for anchor, repeatable
	OriginInfos []OriginInfo `xml:"mdWrap>xmlData>mods>originInfo"`             // required for root, repeatable
	NameInfos   []NameInfo   `xml:"mdWrap>xmlData>mods>name"`                   // required for root, repeatable
}

// TitleInfo ...
type TitleInfo struct {
	Title string `xml:"title" json:"title,omitempty" bson:"title,omitempty"` // required, not repeatable
}

// NameInfo ...
type NameInfo struct {
	// Type         string   `xml:"type,attr",json:"type,omitempty"`
	NameParts    []NamePart `xml:"namePart" json:"namePart,omitempty" bson:"namePart,omitempty"`          // optinal, repeatable
	Roles        []Role     `xml:"role" json:"role,omitempty" bson:"role,omitempty"`                      // optinal, repeatable
	DisplayForms []string   `xml:"displayForm" json:"displayForm,omitempty" bson:"displayForm,omitempty"` // optinal, repeatable
}

// NamePart ...
type NamePart struct {
	Value string `xml:",chardata" json:"value,omitempty" bson:"value,omitempty"` //
	Type  string `xml:"type,attr" json:"type,omitempty"`
}

// Role ...
type Role struct {
	RoleTerms []RoleTerm `xml:"roleTerm" json:"role_term,omitempty" bson:"role_term,omitempty"` // optinal, repeatable
}

// RoleTerm ...
type RoleTerm struct {
	Value     string `xml:",chardata" json:"value,omitempty" bson:"value,omitempty"` //
	Authority string `xml:"authority,attr" json:"authority,omitempty"`
	Type      string `xml:"type,attr" json:"type,omitempty"`
}

// OriginInfo ...
type OriginInfo struct {
	Places             []Place       `xml:"place" json:"place,omitempty" bson:"place,omitempty"`                   // optinal, repeatable
	Publishers         []string      `xml:"publisher" json:"publisher,omitempty" bson:"publisher,omitempty"`       // optinal, repeatable
	DateIssuedStrings  []DateIssued  `xml:"dateIssued" json:"dateIssued,omitempty" bson:"dateIssued,omitempty"`    // optinal, repeatable
	DateCreatedStrings []DateCreated `xml:"dateCreated" json:"dateCreated,omitempty" bson:"dateCreated,omitempty"` // optinal, repeatable
	Edition            string        `xml:"edition" json:"edition,omitempty" bson:"edition,omitempty"`             // optinal, repeatable
}

// Place ...
type Place struct {
	PlaceTerms []PlaceTerm `xml:"placeTerm" json:"place_term,omitempty" bson:"place_term,omitempty"` // // required, repeatabley
}

// PlaceTerm ...
type PlaceTerm struct {
	Value string `xml:",chardata" json:"value,omitempty" bson:"value,omitempty"` //
	Type  string `xml:"type,attr" json:"type,omitempty"`
}

// DateIssued ...
type DateIssued struct {
	Value   string `xml:",chardata" json:"value,omitempty" bson:"value,omitempty"` //
	KeyDate string `xml:"keyDate,attr" json:"keyDate,omitempty"`
}

// DateCreated ...
type DateCreated struct {
	Value   string `xml:",chardata" json:"value,omitempty" bson:"value,omitempty"` //
	KeyDate string `xml:"keyDate,attr" json:"keyDate,omitempty"`
}

// DmdsecRecordID ...
type DmdsecRecordID struct {
	XMLName xml.Name `xml:"dmdSec"`
	ID      string   `xml:"ID,attr" json:"ID"`
}

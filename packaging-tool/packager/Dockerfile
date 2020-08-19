FROM golang:1.14
RUN apt-get -y update && apt-get install -y  git libmagickwand-dev && rm -rf /var/lib/apt/lists/*
RUN pkg-config --cflags --libs MagickWand

RUN go get gopkg.in/gographics/imagick.v2/imagick

# workaround because of the fact github.com/pierrec/lz4/v3 is not go module compatible
#RUN GO111MODULE=on go get github.com/pierrec/lz4/v3 && \
#  mkdir -p $GOPATH/src/github.com/pierrec/lz4/v3@v3.3.2 && \
#  cp -r $GOPATH/pkg/mod/github.com/pierrec/lz4/v3@v3.3.2 $GOPATH/src/github.com/pierrec/lz4/v3@v3.3.2 && \
#  ls -l $GOPATH/src/github.com/pierrec/lz4/v3@v3.3.2 

RUN go get github.com/pierrec/lz4 && \
    cd $GOPATH/src/github.com/pierrec/lz4 && \
    git fetch && \ 
    git checkout v3.3.2

WORKDIR /go/src/api
COPY . .

RUN go get -v . 
RUN go build -o goapp

#RUN mkdir -p $GOPATH/src/github.com/ && \
#    cd $GOPATH/src/github.com/ && \
#    git clone https://github.com/pierrec/lz4.git && \
#    cd lz4 && \
#    git checkout v3  && \
#    go install github.com/pierrec/lz4 && \
#    cd /go/src/api && \
#    go get . && \
#    go build -o goapp


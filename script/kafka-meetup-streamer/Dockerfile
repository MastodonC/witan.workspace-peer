FROM ubuntu:14.04

WORKDIR /build

ENV BUILD_PACKAGES "build-essential git curl zlib1g-dev python"
RUN apt-get update -y
RUN apt-get install $BUILD_PACKAGES -y
RUN git clone https://github.com/edenhill/kafkacat.git && \
cd kafkacat && \
./bootstrap.sh && \
make install && \
cd .. && rm -rf kafkacat && \
rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ADD script.sh script.sh
RUN chmod +x script.sh

CMD ["./script.sh"]

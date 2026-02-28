# Dockerfile to build container for unit testing.
#
# To build the image, run the following from this directory:
#   docker build -t beast_testing .
#
# To run the tests (fast tests only, slow MCMC tests excluded), use
#   docker run beast_testing
#
# To run all tests including slow MCMC tests, use
#   docker run beast_testing -Pslow-tests
#
# To run the tests interactively, use
#   docker run --entrypoint /bin/bash -it -p 5900:5900 beast_testing
# This will give you a shell in the container. From this
# shell, run
#   vncserver $DISPLAY -geometry 1920x1080; mvn test
#
# The previous command exposes the VNC session, so while the
# BEAUti test suite is running you can run a VNC viewer and
# connect it to localhost (password: password) to observe
# the graphical output of these tests.

FROM debian:stable
WORKDIR /beast3

# Install JDK 25 and Maven
RUN apt-get update && apt-get install -y wget maven
RUN wget -q https://cdn.azul.com/zulu/bin/zulu25.32.21-ca-jdk25.0.2-linux_x64.tar.gz -O /tmp/jdk.tar.gz \
    && mkdir -p /usr/lib/jvm \
    && tar xzf /tmp/jdk.tar.gz -C /usr/lib/jvm \
    && rm /tmp/jdk.tar.gz
ENV JAVA_HOME=/usr/lib/jvm/zulu25.32.21-ca-jdk25.0.2-linux_x64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install and configure VNC server
RUN apt-get update && apt-get install -y tightvncserver twm
RUN mkdir /root/.vnc
RUN echo password | vncpasswd -f > /root/.vnc/passwd
RUN chmod 600 /root/.vnc/passwd

# Install BEAGLE
RUN apt-get update && apt-get install -y build-essential autoconf automake libtool pkg-config git
# use latest release v3.1.2, issue #786
RUN cd /root && git clone --branch v3.1.2 --depth=1 https://github.com/beagle-dev/beagle-lib.git
RUN cd /root/beagle-lib && ./autogen.sh && ./configure --prefix=/usr/local && make install
RUN ldconfig

ADD . ./

# Install local-only modular JARs (beagle.jar, colt.jar)
RUN mvn install:install-file -Dfile=lib/beagle.jar -DgroupId=io.github.alexeid -DartifactId=beagle -Dversion=1.0 -Dpackaging=jar -q
RUN mvn install:install-file -Dfile=lib/colt.jar -DgroupId=io.github.alexeid -DartifactId=colt -Dversion=1.0 -Dpackaging=jar -q

RUN echo "#!/bin/bash\n" \
        "export USER=root\n" \
        "export DISPLAY=:1\n" \
        "vncserver :1 -geometry 1920x1080\n" \
        "mvn test \$@\n" > entrypoint.sh
RUN chmod a+x entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]

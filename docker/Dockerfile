FROM registry1.dso.mil/ironbank/redhat/ubi/ubi8:8.6-fips
ARG USER=1001
ARG JAVA_OPTS="-XX:MaxRAMPercentage=80"
ENV JAVA_OPTS=${JAVA_OPTS}

USER root

RUN dnf upgrade -y && \
    rpm -ivh https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm && \
    rpm -ivh http://mirrors.ocf.berkeley.edu/centos/8-stream/BaseOS/x86_64/os/Packages/centos-gpg-keys-8-6.el8.noarch.rpm http://mirrors.ocf.berkeley.edu/centos/8-stream/BaseOS/x86_64/os/Packages/centos-stream-repos-8-6.el8.noarch.rpm && \
    dnf install --nodocs -y glibc-langpack-en && \
    dnf install --disablerepo=ubi-8-appstream --nodocs -y \
    java-17-openjdk \
    tigervnc-server \
    xrdp \
    xorgxrdp \
    supervisor \
    openbox \
    rxvt-unicode \
    wget \
    python3-numpy \
    nodejs \
    nodejs-full-i18n \
    xfce4-panel xfwm4 xfce4-settings xfce4-session xfdesktop xfce4-terminal && \
    dnf clean all

RUN rpm -ivh http://mirrors.ocf.berkeley.edu/centos/8-stream/cloud/x86_64/openstack-xena/Packages/p/python3-websockify-0.9.0-1.el8.noarch.rpm
RUN dnf install -y novnc    

# Fix to be allowed to start X for xrdp when not running on a physical TTY.
RUN echo "allowed_users = anybody" >> /etc/X11/Xwrapper.config

# Copy configuration files for xrdp.
COPY etc/xrdp/xrdp.ini     /etc/xrdp/xrdp.ini
COPY etc/xrdp/sesman.ini   /etc/xrdp/sesman.ini

# Create self-signed certificate for noVNC.
RUN openssl req -x509 -nodes -newkey rsa:4096 -days 365000 \
    -keyout /etc/pki/tls/certs/novnc.pem -out /etc/pki/tls/certs/novnc.pem  \
    -subj "/C=US/ST=California/L=Livermore/O=LLNL/OU=GMP/CN=CCT"

# Configure xrdp to start openbox on user login
RUN echo "exec openbox-session" > ~/startwm.sh && chmod +x ~/startwm.sh

RUN useradd -u ${USER} cct && groupadd logging && usermod -aG logging cct && \
    chown root:logging /var/log && chmod 770 /var/log && \
    mkdir -p /opt/cct && chmod 777 /opt/cct && \
    mkdir -p /opt/cct/.vnc && chmod 777 /opt/cct/.vnc && \
    chmod -R 755 /etc/pki/tls/certs/novnc.pem && \
    touch /opt/cct/.bashrc && echo 'cd ${HOME}' >> /opt/cct/.bashrc

COPY xclients /opt/cct/.Xclients

RUN chown -R cct:cct /home/cct && chmod -R 777 /home/cct

USER cct

ADD calibration-standalone-*runnable.jar /opt/cct/coda-calibration-standalone.jar
ADD run.sh /run.sh

EXPOSE 8080
CMD ["/bin/bash", "/run.sh"]

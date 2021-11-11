FROM registry1.dso.mil/ironbank/redhat/ubi/ubi8:8.4
ARG USER=1001
USER root

#The UBI appstream repo only has the headless java so we manually install the full package
RUN rpm -ivh https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm && \
    rpm -ivh http://mirror.centos.org/centos/8-stream/BaseOS/x86_64/os/Packages/centos-gpg-keys-8-3.el8.noarch.rpm http://mirror.centos.org/centos/8/extras/x86_64/os/Packages/centos-stream-repos-8-3.el8.noarch.rpm && \
    dnf install --nodocs -y glibc-langpack-en && \
    dnf install --disablerepo=ubi-8-appstream --nodocs -y \
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
    xfce4-panel xfwm4 xfce4-settings xfce4-session xfdesktop && \
    dnf install -y --nogpgcheck http://mirror.centos.org/centos/8/AppStream/x86_64/os/Packages/java-11-openjdk-11.0.12.0.7-0.el8_4.x86_64.rpm && \
    wget --no-check-certificate https://cbs.centos.org/kojifiles/packages/novnc/1.2.0/2.el8/noarch/novnc-1.2.0-2.el8.noarch.rpm -O /tmp/novnc.rpm && \
    wget --no-check-certificate https://cbs.centos.org/kojifiles/packages/python-websockify/0.9.0/1.el8/noarch/python3-websockify-0.9.0-1.el8.noarch.rpm -O /tmp/sockify.rpm && \    
    rpm -ivh /tmp/*rpm && \
    rm -f /tmp/*rpm && \
    dnf clean all

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
    chmod -R 755 /etc/pki/tls/certs/novnc.pem

COPY xclients /opt/cct/.Xclients

RUN chown -R cct:cct /home/cct && chmod -R 777 /home/cct

USER cct

ADD calibration-standalone-*runnable.jar /opt/cct/coda-calibration-standalone.jar
ADD run.sh /run.sh

EXPOSE 8080
CMD ["/bin/bash", "/run.sh"]
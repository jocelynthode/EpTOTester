# Cluster Setup Instructions

These instructions explain how to setup a remote cluster with Docker 1.12 to run EpTO tests. Debian is used on our virtual machines managed by OpenNebula.

##  Setup the image

1. Install Debian stable
2. Install the kernel from the backports
3. Install Docker >= 1.12
4. Create /etc/systemd/system/docker.service.d/docker.conf with the following content:

    ```
    [Service]
    ExecStart=
    ExecStart=/usr/bin/docker daemon -H fd:// -H tcp://0.0.0.0:2375 -H unix:///var/run/docker.sock
    TasksMax=infinity
    ```
    
    :warning: When running on an environment where IPs in the 172.16.0.0/12 subnet might be in use, it is wise to tell Docker to use another RFC 1918 subnet. Example to add to ExecStart (note that it is a machine IP, not the network IP):
    
    ```
    --bip=10.93.0.1/24
    ```
    
5. Install opennebula-context

## Configure the System on OpenNebula
Copy this image on OpenNebula.

### All machines
1. Change the hostname in /etc/{hosts,hostname} to be different on each machine


### Master
1. Generate a private ssh key 

  ```
  ssh-keygen
  ```
2. Copy it to every worker

  ```
  for i (X Y Z); do
    ssh-copy-id debian@{IP}.${i}
  done
  ```

## Running the benchmarks
TODO create a repository to push/pull images
finish script to launch here

# Cluster Setup Instructions

These instructions explain how to setup a remote cluster with Docker 1.12 to run EpTO tests. Debian is used on our virtual machines managed by OpenNebula.

##  Setup the image

1. Install Debian stable
2. Install the kernel from the backports
3. Install Docker >= 1.12
4. Install opennebula-context

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

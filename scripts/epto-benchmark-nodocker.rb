#!/usr/bin/env ruby


ports = [*10000..10050]

puts 'compiling...'
puts `cd ..; ./gradlew --daemon clean shadowJar && cd scripts/;`
# Replace each of the dummy IP by the ips of the vms (don't forget the ':')
ips = %w(1.1.1.1: 1.1.1.2: 1.1.1.3: 1.1.1.4:)
# Here indicate the ip of the machine on which this script is started
own_ip = '1.1.1.1:'

args = ports.map { |port| [ips.map { |ip| ip + port.to_s }] }
args.flatten!

(0..50).each { |i|
  complete_ip = own_ip + ports[i].to_s
  args.delete(complete_ip)
  args.insert(0, complete_ip)
  args = args.join(' ')

  puts `java -cp ../build/libs/epto-1.0-SNAPSHOT-all.jar epto.utilities.App #{args} > localhost#{i}.txt 2>&1 &`
}

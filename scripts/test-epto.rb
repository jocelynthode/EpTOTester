#!/usr/bin/env ruby

ports = [*10000..10059]

(0..59).each { |i|
  args = ports.map { |port| 'localhost:' + port.to_s }
  ports.rotate!
  args = args.join(' ')
  puts "java -cp ../lib/neem-0.8-jar-with-dependencies.jar:../build/libs/epto-1.0-SNAPSHOT.jar epto.utilities.App #{args} > localhost#{i}.txt 2>&1 &\n"
  `java -cp ../lib/neem-0.8-jar-with-dependencies.jar:../build/libs/epto-1.0-SNAPSHOT.jar epto.utilities.App #{args} > localhost#{i}.txt 2>&1 &`
}

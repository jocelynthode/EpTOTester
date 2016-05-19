#!/usr/bin/env ruby

ports = [*10000..10019]

puts 'compiling...'
puts `cd ..; ./gradlew --daemon clean shadowJar && cd scripts/;`

(0..19).each { |i|
  args = ports.map { |port| 'localhost:' + port.to_s }
  ports.rotate!
  args = args.join(' ')
  puts "java -cp ../build/libs/epto-1.0-SNAPSHOT-all.jar epto.utilities.App #{args} > localhost#{i}.txt 2>&1 &\n"
  `java -cp ../build/libs/epto-1.0-SNAPSHOT-all.jar epto.utilities.App #{args} > localhost#{i}.txt 2>&1 &`
}

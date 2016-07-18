#!/usr/bin/env ruby


ids = [*1..25]

puts 'compiling...'
puts `cd ..; ./gradlew --daemon clean shadowJar && cd scripts/;`

args = ids.map { |id| 'eptoneem_epto_neem_' + id.to_s + ':10000'}
#ports.rotate!
args = args.join(' ')
puts "java -cp ../build/libs/epto-1.0-SNAPSHOT-all.jar epto.utilities.App #{args} > localhost.txt 2>&1 &\n"
`java -cp ../build/libs/epto-1.0-SNAPSHOT-all.jar epto.utilities.App #{args} > localhost.txt 2>&1 &`

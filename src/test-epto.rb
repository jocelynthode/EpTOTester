#!/usr/bin/env ruby

puts 'compiling...'
puts 'javac -cp ../lib/neem-0.8-jar-with-dependencies.jar:src src/epto/utilities/App.java'
`javac -cp ../lib/neem-0.8-jar-with-dependencies.jar:src src/epto/utilities/App.java`


ports = [*8000..8019]

(0..19).each { |i|
    args = ports.map { |port| 'localhost:' + port.to_s }
    ports.rotate!
    args = args.join(' ')
    puts "java -cp ../lib/neem-0.8-jar-with-dependencies.jar:src epto.utilities.App #{args} > localhost#{i}.txt &"
    `java -cp ../lib/neem-0.8-jar-with-dependencies.jar:src epto.utilities.App #{args} > localhost#{i}.txt &`
}

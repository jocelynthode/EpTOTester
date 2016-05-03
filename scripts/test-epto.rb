#!/usr/bin/env ruby

ports = [*8000..8001]

(0..1).each { |i|
    args = ports.map { |port| 'localhost:' + port.to_s }
    ports.rotate!
    args = args.join(' ')
    puts "./gradlew run #{args} > localhost#{i}.txt 2>&1 &\n"
    `./gradlew run #{args} > localhost#{i}.txt 2>&1 &`
}

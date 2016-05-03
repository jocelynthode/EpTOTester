#!/usr/bin/env ruby

ports = [*8000..8019]

(0..19).each { |i|
    args = ports.map { |port| 'localhost:' + port.to_s }
    ports.rotate!
    args = args.join(' ')
    puts "./gradlew --daemon run #{args} > localhost#{i}.txt 2>&1 &\n"
    `./gradlew --daemon run #{args} > localhost#{i}.txt 2>&1 &`
}

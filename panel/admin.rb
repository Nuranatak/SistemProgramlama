require 'socket'

servers = [
  { host: 'localhost', port: 5001 },
  { host: 'localhost', port: 5002 },
  { host: 'localhost', port: 5003 }
]

config = {}
File.foreach('dist_subs.conf') do |line|
  key, value = line.split('=').map(&:strip)
  config[key] = value
end

puts "Fault Tolerance Level: #{config['fault_tolerance_level']}"
puts "Method: #{config['method']}"

servers.each do |server|
  begin
    socket = TCPSocket.new(server[:host], server[:port])
    puts "Connected to #{server[:host]}:#{server[:port]}"

    socket.puts("demand=#{config['method']}, fault_tolerance=#{config['fault_tolerance_level']}")
    response = socket.gets.strip
    puts "Response: #{response}"
    socket.close
  rescue Errno::ECONNREFUSED
    puts "Failed to connect to #{server[:host]}:#{server[:port]}"
  end
end

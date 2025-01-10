require_relative 'configuration'
require 'socket'

# Hata tolerans seviyesini dosyadan okuma
def read_fault_tolerance_level(file_path)
  File.foreach(file_path) do |line|
    return $1.to_i if line =~ /fault_tolerance_level = (\d+)/
  end
  nil
end

# Sunucuya mesaj gönderme
def send_message_to_server(server, demand)
  puts "Sunucu: #{server[:name]} | İstek: '#{demand}' | Port: #{server[:port]}"
  begin
    socket = TCPSocket.new('localhost', server[:port])
    socket.puts(demand.to_s)
    response = socket.gets
    socket.close
    puts "Yanıt: #{response}"
    response
  rescue SocketError => e
    puts "Socket hatası: #{e.message}"
  rescue StandardError => e
    puts "Hata: #{e.message}"
  end
  nil
end

# Plotter'a kapasite gönderme
def send_capacity_to_plotter(capacity)
  begin
    TCPSocket.open('localhost', 7000) { |socket| socket.puts(capacity) }
    puts "Plotter'a kapasite gönderildi: #{capacity}"
  rescue StandardError => e
    puts "Plotter'a gönderimde hata: #{e.message}"
  end
end

# Kapasite bilgisini sunucudan al ve plotter'a gönder
def get_capacity_info(server)
  response = send_message_to_server(server, "CPCTY")
  if response && response =~ /server1_status:(\d+)/
    capacity = $1.to_i
    send_capacity_to_plotter(capacity)
  else
    puts "#{server[:name]} yanıt vermedi veya format hatalı."
  end
end

# Sunucuların kapasitesini periyodik olarak sorgula
def request_capacity_periodically(servers)
  Thread.new do
    loop do
      servers.each { |server| get_capacity_info(server) }
      sleep 5
    end
  end
end

# Hata toleransı seviyesini belirli porta gönder
def send_fault_tolerance_to_server_at_port(fault_tolerance_level, port)
  begin
    TCPSocket.open('localhost', port) { |socket| socket.puts("FAULT_TOLERANCE:#{fault_tolerance_level}") }
    puts "Hata toleransı seviyesi gönderildi: #{fault_tolerance_level} | Port: #{port}"
  rescue StandardError => e
    puts "Hata toleransı gönderim hatası: #{e.message}"
  end
end

# Tüm sunuculara hata toleransı seviyesini gönder
def send_fault_tolerance_to_servers(servers, fault_tolerance_level)
  servers.each_with_index do |_, index|
    port = 6001 + index
    send_fault_tolerance_to_server_at_port(fault_tolerance_level, port)
  end
end

# Ana program
def main
  config_file_path = 'dist_subs.conf'
  fault_tolerance_level = read_fault_tolerance_level(config_file_path)

  if fault_tolerance_level
    puts "Hata tolerans seviyesi: #{fault_tolerance_level}"
    servers = [
      { name: 'Server1', port: 5001 },
      { name: 'Server2', port: 5002 },
      { name: 'Server3', port: 5003 }
    ]

    send_fault_tolerance_to_servers(servers, fault_tolerance_level)
    request_capacity_periodically(servers)

    server_socket = TCPServer.new(6000)
    puts "Admin dinleme portu (6000) aktif..."

    loop do
      client_socket = server_socket.accept
      puts "Bağlantı kabul edildi."
      capacity_message = client_socket.gets.chomp
      puts "Alınan kapasite mesajı: #{capacity_message}"
      send_capacity_to_plotter(capacity_message)
      client_socket.close
    end
  else
    puts "Hata: 'fault_tolerance_level' dosyadan okunamadı."
  end
end

main

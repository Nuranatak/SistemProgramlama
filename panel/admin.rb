require 'socket'
require_relative 'configuration_pb'
require_relative 'message_pb'
require_relative 'capacity_pb'

PLOTTER_HOST = 'localhost'
PLOTTER_PORT = 6000 # plotter.py için farklı bir port tanımladık

# Kapasite durumu isteği gönderme ve yanıtları toplama
def query_capacity
  servers = [
    { host: 'localhost', port: 5001 },
    { host: 'localhost', port: 5002 },
    { host: 'localhost', port: 5003 }
  ]

  loop do
    servers.each do |server|
      begin
        socket = TCPSocket.new(server[:host], server[:port])
        puts "Connected to server on port #{server[:port]} for capacity query"

        # CPCTY talebini gönder
        request = Message.new(demand: Message::Demand::CPCTY, response: nil)
        socket.write(request.to_proto)

        # Capacity yanıtını al
        response = Capacity.parse_from(socket.read)
        puts "Server #{server[:port]} Capacity: #{response.server_status}, Timestamp: #{response.timestamp}"

        # Kapasite bilgilerini plotter.py'ye gönderin
        send_to_plotter(response)

        socket.close
      rescue => e
        puts "Failed to connect to server on port #{server[:port]}: #{e.message}"
      end
    end
    sleep 5 # Her 5 saniyede bir sorgulama yap
  end
end

# plotter.py'ye veri gönderme fonksiyonu
def send_to_plotter(capacity_data)
  begin
    plotter_socket = TCPSocket.new(PLOTTER_HOST, PLOTTER_PORT)
    plotter_socket.write(capacity_data.to_proto) # kapasite verisini gönder
    plotter_socket.close
  rescue => e
    puts "Failed to send data to plotter: #{e.message}"
  end
end

# Kapasite sorgulama döngüsünü başlat
query_capacity

import matplotlib.pyplot as plt
import matplotlib.animation as animation
import socket
from capacity_pb2 import Capacity

# plotter.py sunucu ayarları
HOST = 'localhost'
PORT = 6000

# Grafik oluşturma
fig, ax = plt.subplots()
x_data, y_data_1, y_data_2, y_data_3 = [], [], [], []

def update_graph(frame):
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.bind((HOST, PORT))
            s.listen(1)
            print("Plotter server is listening on port", PORT)

            conn, addr = s.accept()
            with conn:
                print("Connected by", addr)
                
                # Kapasite verisini al ve protobuf nesnesine dönüştür
                data = conn.recv(1024)
                capacity = Capacity()
                capacity.ParseFromString(data)

                # Sunucuya göre kapasite verilerini ayır
                timestamp = capacity.timestamp
                if addr[1] == 5001:
                    y_data_1.append(capacity.server_status)
                elif addr[1] == 5002:
                    y_data_2.append(capacity.server_status)
                elif addr[1] == 5003:
                    y_data_3.append(capacity.server_status)

                x_data.append(timestamp)

                # Grafik güncelleme
                ax.clear()
                ax.plot(x_data, y_data_1, label="Server 1 Capacity")
                ax.plot(x_data, y_data_2, label="Server 2 Capacity")
                ax.plot(x_data, y_data_3, label="Server 3 Capacity")
                ax.set_title("Server Capacity Over Time")
                ax.set_xlabel("Timestamp")
                ax.set_ylabel("Capacity")
                ax.legend()
    except Exception as e:
        print(f"Failed to receive data: {e}")

ani = animation.FuncAnimation(fig, update_graph, interval=5000)
plt.show()

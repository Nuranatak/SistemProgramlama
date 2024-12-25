import socket
import matplotlib.pyplot as plt
import threading
import time
from collections import defaultdict
from queue import Queue

# Sunuculardan gelen kapasite verileri
capacity_data = defaultdict(list)
timestamps = set()  # Zaman damgalarını benzersiz tutmak için set kullanıyoruz
timestamp_list = []  # Zaman damgalarının sıralı hali

# Plotter sunucu ayarları
HOST = "localhost"
PORT = 6000

# Grafik güncelleme hızı (saniye cinsinden)
PLOT_UPDATE_INTERVAL = 0.1

# Veri kuyruğu
data_queue = Queue()

# Sunucu kapasitelerini işleyen sunucu fonksiyonu
def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        server_socket.bind((HOST, PORT))
        server_socket.listen(5)
        print(f"Plotter server running on {HOST}:{PORT}...")

        while True:
            client_socket, addr = server_socket.accept()
            threading.Thread(target=handle_client, args=(client_socket, addr), daemon=True).start()

# İstemciden gelen veriyi işleyen fonksiyon
def handle_client(client_socket, addr):
    with client_socket:
        print(f"Connection from {addr}")
        try:
            data = client_socket.recv(1024).decode()
            if data:
                data_queue.put(data)  # Gelen veriyi kuyruğa ekle
        except Exception as e:
            print(f"Error receiving data from {addr}: {e}")

# Gelen kapasite verilerini işleyen fonksiyon
def process_data():
    while True:
        data = data_queue.get()  # Kuyruğa eklenen veriyi al
        if data:
            try:
                server_name, capacity, timestamp = data.split(",")
                capacity = int(capacity)
                timestamp = int(timestamp)

                # Kapasite verilerini güncelle
                capacity_data[server_name].append(capacity)

                # Zaman damgasını ekle (sadece benzersiz zaman damgası)
                if timestamp not in timestamps:
                    timestamps.add(timestamp)
                    timestamp_list.append(timestamp)

            except ValueError as e:
                print(f"Error processing data: {e}")
            except Exception as e:
                print(f"Unexpected error: {e}")

# Grafik güncellemelerini yöneten fonksiyon
def update_graph():
    plt.ion()  # Etkileşimli mod
    while True:
        plt.clf()
        for server, capacities in capacity_data.items():
            if len(capacities) > 0:
                plt.plot(
                    timestamp_list[:len(capacities)],
                    capacities,
                    marker="o",
                    label=server
                )
        plt.xlabel("Timestamp")
        plt.ylabel("Capacity")
        plt.title("Server Capacities Over Time")
        plt.legend()
        plt.pause(PLOT_UPDATE_INTERVAL)

# Ana fonksiyon
def main():
    # Grafik güncelleme iş parçacığı
    plot_thread = threading.Thread(target=update_graph, daemon=True)
    plot_thread.start()

    # Veri işleme iş parçacığı
    data_thread = threading.Thread(target=process_data, daemon=True)
    data_thread.start()

    # Sunucu iş parçacığını başlat
    start_server()

if __name__ == "__main__":
    main()

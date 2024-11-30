import socket
import matplotlib.pyplot as plt
import threading
import time

capacity_data = {"Server1": [], "Server2": [], "Server3": []}
timestamps = []

def start_server():
    host = "localhost"
    port = 6000

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        server_socket.bind((host, port))
        server_socket.listen(5)
        print(f"Plotter server running on {host}:{port}...")

        while True:
            client_socket, addr = server_socket.accept()
            with client_socket:
                print(f"Connection from {addr}")
                data = client_socket.recv(1024).decode()
                if data:
                    process_data(data)

def process_data(data):
    global timestamps
    server_name, capacity, timestamp = data.split(",")
    capacity = int(capacity)
    timestamp = int(timestamp)

    capacity_data[server_name].append(capacity)
    if len(timestamps) == 0 or timestamp != timestamps[-1]:
        timestamps.append(timestamp)

    update_graph()

def update_graph():
    plt.clf()
    for server, capacities in capacity_data.items():
        if len(capacities) > 0:
            plt.plot(
                timestamps[:len(capacities)],
                capacities,
                marker="o",
                label=server
            )
    
    plt.xlabel("Timestamp")
    plt.ylabel("Capacity")
    plt.title("Server Capacities Over Time")
    plt.legend()
    plt.pause(0.1)

def start_plot():
    plt.ion()
    plt.show()

def main():
    plot_thread = threading.Thread(target=start_plot, daemon=True)
    plot_thread.start()
    start_server()

if __name__ == "__main__":
    main()

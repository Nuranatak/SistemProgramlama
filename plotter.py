import socket
import matplotlib.pyplot as plt
from collections import defaultdict
import threading
import tkinter as tk
from tkinter import Frame
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg

# Küresel değişkenler
capacity_data = defaultdict(list)
colors = ['b', 'g', 'r', 'c', 'm', 'y', 'k']  
server_names = ['Server1', 'Server2', 'Server3'] 

class PlotterApp:
    def __init__(self, master):
        self.master = master
        self.master.title("Server Capacity Plotter")
        self.master.protocol("WM_DELETE_WINDOW", self.on_closing)

        self.frame = Frame(master)
        self.frame.pack(fill=tk.BOTH, expand=True)

        # Matplotlib figür ve eksenleri
        self.figure, self.axes = plt.subplots(len(server_names), 1, figsize=(8, 6), dpi=100)
        self.figure.tight_layout(pad=3.0)
        self.canvas_widget = FigureCanvasTkAgg(self.figure, master=self.frame)
        self.canvas_widget.get_tk_widget().pack(side=tk.TOP, fill=tk.BOTH, expand=True)

        self.running = True
        self.start_plotter_server()

    def start_plotter_server(self):
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.bind(('localhost', 7000)) 
        server_socket.listen(5)
        print("Plotter server is listening on port 7000...")

        # Sunucu bağlantılarını işlemek için bir thread başlatılıyor
        threading.Thread(target=self.accept_connections, args=(server_socket,), daemon=True).start()

    def accept_connections(self, server_socket):
        while self.running:
            try:
                client_socket, addr = server_socket.accept()
                print(f"Connection from {addr} has been established!")
                data = client_socket.recv(1024).decode('utf-8')
                if data:
                    threading.Thread(target=self.handle_data, args=(data,), daemon=True).start()
                client_socket.close()
            except socket.error as e:
                print(f"Socket error: {e}")

    def handle_data(self, data):
        try:
            server_name, capacity = data.split(':')  
            capacity = int(capacity)
            if server_name in server_names:
                capacity_data[server_name].append(capacity)  
                print(f"Received data: {server_name} - {capacity}")
                self.plot_data()
            else:
                print(f"Unknown server name: {server_name}")
        except ValueError as e:
            print(f"Error processing data '{data}': {e}")

    def plot_data(self):
        for i, server in enumerate(server_names):
            self.axes[i].clear()  
            if server in capacity_data:
                color = colors[i % len(colors)] 
                self.axes[i].plot(capacity_data[server], label=server, color=color)

            self.axes[i].set_title(f"{server} Capacity Over Time")
            self.axes[i].set_xlabel("Time (5s intervals)")
            self.axes[i].set_ylabel("Capacity")
            self.axes[i].legend()

        self.canvas_widget.draw()  

    def on_closing(self):
        self.running = False
        self.master.destroy()
        print("Application closed.")

if __name__ == "__main__":
    import matplotlib

    matplotlib.use("TkAgg")

    root = tk.Tk()
    app = PlotterApp(root)
    root.mainloop()

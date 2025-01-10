# Dağıtık Abonelik Sistemi (Distributed Subscriber Service)

Bu proje, dağıtık bir mimari üzerinde hata toleranslı bir abonelik sistemi geliştirmeyi amaçlamaktadır. Sistem, birden fazla sunucunun koordinasyon halinde çalışarak yük paylaşımı, hata toleransı ve gerçek zamanlı izleme gibi özellikleri sağlamasını içerir.

---

### ServerX.java özellikleri

- [x] admin_client.rb ile başlama
- [x] Hata toleransı 1 prensibiyle çalışma
- [x] Sunucu yüklerini gerçek zamanlı paylaşma
- [x] Peer-to-peer bağlantılar kurarak diğer sunucularla iletişim
- [x] Threading ile çoklu istemci desteği
- [ ] Kapasite bilgilerini veritabanına yazma (opsiyonel)
- [ ] Sunucular arası yük dengeleme optimizasyonu

---

### plotter.py özellikleri

- [x] Gerçek zamanlı yük durumlarını grafiksel gösterim
- [x] Sunucuların kapasite bilgilerini görselleştirme
- [x] Admin istemciden veri çekme
- [ ] Web tabanlı bir arayüzle entegrasyon
- [ ] Geçmiş yük verilerinin analizi için veri kaydı

---

### admin.rb özellikleri

- [x] `dist_subs.conf` dosyasından hata tolerans seviyesini okuma
- [x] Tüm sunuculara `STRT` komutunu gönderme
- [x] Sunucuların kapasite bilgilerini sorgulama
- [x] Hata tolerans seviyesine göre dinamik istemci yönlendirme
- [ ] Sunucuları yeniden başlatma mekanizması ekleme
- [ ] Loglama ve durum raporları oluşturma

---

### Ekip Üyeleri

- 123456, Ahmet Yılmaz
- 234567, Ayşe Kaya
- 345678, Mehmet Çelik
- 456789, Elif Doğan

---

### Sunum Videosu Linki

Ekip üyeleri Google Meet eşliğinde projeyi anlatmalıdır. Video içeriği aşağıdaki formatta olmalıdır:

1. **Giriş:** Ekip üyeleri isimlerini, numaralarını ve teknik ilgi alanlarını tanıtmalıdır.
2. **Proje Sunumu:** Maksimum 3 dakika içinde kodların çalıştırılması ve logların gösterimi yapılmalıdır.

**Not:** Video bağlantısı dersin hocası ve asistanı tarafından erişilebilir olmalıdır.

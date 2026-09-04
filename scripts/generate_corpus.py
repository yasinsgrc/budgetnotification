#!/usr/bin/env python3
"""
Test korpusu ureteci.

ONEMLI - dongusellik uyarisi:
Beklenen degerleri (tutar, isyeri, tip) ayristiricinin ciktisindan DEGIL,
metni olustururken kullandigimiz girdilerden aliyoruz. Yani once "MIGROS,
245,90 TL, harcama" diye karar veriyoruz, sonra bunu bir banka sablonuna
yerlestiriyoruz. Ayristirici bu karari geri bulabiliyor mu, testi budur.

Uretilen metinler GERCEK degil, gercege benzetilmis sablonlardir. Yayina
cikmadan once gercek bildirimlerle degistirilmeli/desteklenmelidir
(scripts/add-fixture.sh).

Kullanim:  python3 scripts/generate_corpus.py > parser/src/test/resources/fixtures.tsv

DIKKAT - yukaridaki '>' dosyayi bastan yazar, add-fixture.sh ile eklenmis
GERCEK ornekleri siler. Korpusu yeniden uretirken REAL satirlari koruyun:

    F=parser/src/test/resources/fixtures.tsv
    grep -P '\\tREAL$' "$F" > /tmp/real.tsv || true
    python3 scripts/generate_corpus.py > /tmp/new.tsv
    cat /tmp/real.tsv >> /tmp/new.tsv && mv /tmp/new.tsv "$F"
"""
import random
import sys

random.seed(20260821)  # deterministik: ayni korpus her calistirmada uretilir

# (ham metin icindeki yazim, beklenen goruntuleme adi)
MERCHANTS = [
    ("MIGROS", "Migros"),
    ("MIGROS TICARET A.S.", "Migros Ticaret A.S"),
    ("A101 YENI MAGAZACILIK", "A101 Yeni Magazacilik"),
    ("BIM", "BIM"),
    ("CARREFOURSA", "Carrefoursa"),
    ("SOK MARKETLER", "Sok Marketler"),
    ("GETIR PERAKENDE", "Getir Perakende"),
    ("STARBUCKS", "Starbucks"),
    ("DOMINOS PIZZA", "Dominos Pizza"),
    ("BURGER KING", "Burger King"),
    ("YEMEKSEPETI", "Yemeksepeti"),
    ("SHELL PETROL", "Shell Petrol"),
    ("OPET AKARYAKIT", "Opet Akaryakit"),
    ("BITAKSI", "Bitaksi"),
    ("TRENDYOL", "Trendyol"),
    ("HEPSIBURADA", "Hepsiburada"),
    ("LC WAIKIKI", "LC Waikiki"),
    ("MEDIAMARKT", "Mediamarkt"),
    ("TEKNOSA", "Teknosa"),
    ("IKEA", "IKEA"),
    ("BOYNER", "Boyner"),
    ("TURKCELL", "Turkcell"),
    ("NETFLIX", "Netflix"),
    ("SPOTIFY AB", "Spotify AB"),
    ("ENERJISA", "Enerjisa"),
    ("SIFA ECZANESI", "Sifa Eczanesi"),
    ("ACIBADEM HASTANESI", "Acibadem Hastanesi"),
    ("CINEMAXIMUM", "Cinemaximum"),
    ("MACFIT", "Macfit"),
    ("STEAM GAMES", "Steam Games"),
    ("APPLE SERVICES", "Apple Services"),
    ("N11 COM", "N11 COM"),
]

# Tutarlar: kurus cinsinden ground truth -> Turkce bicime cevrilecek
AMOUNTS = [
    899, 1250, 4500, 8990, 12945, 17500, 24590, 34999, 45000, 67550,
    99900, 124900, 189950, 250000, 349900, 1250000, 75, 100, 999999,
]

CARDS = ["1234", "5678", "9012", "4455"]
DATES = ["15.08.2026", "03.07.2026", "21.08.2026"]

# --- Harcama sablonlari -------------------------------------------------
# {m}=isyeri  {a}=tutar  {c}=kart son 4  {d}=tarih  {cur}=para birimi
EXPENSE_TEMPLATES = [
    "Garanti BBVA: {c} kartiniz ile {m} isyerinde {a} {cur} tutarinda harcama yapilmistir.",
    "İş Bankası: Kartınızla {d} tarihinde {m}'ta {a} {cur} harcama yapıldı.",
    "Sayın müşterimiz, {c} nolu kartınızla {m}'da {a} {cur} tutarında işlem gerçekleşti.",
    "Akbank: Kredi kartınızdan {m}'de {a} {cur} harcama yapılmıştır.",
    "Yapı Kredi: World kartınız ile {m} {a} {cur}",
    "Enpara: {a} {cur} tutarında harcamanız {m} bünyesinde gerçekleşti.",
    "DenizBank: Kartınızla {m} mağazasında {a} {cur} tutarında ödeme yapılmıştır.",
    "QNB: Kartınızla {m}'da {a} {cur} tutarında işlem gerçekleşti.",
    "Ziraat Bankası: {c} nolu kartınız ile {m} adlı üye işyerinde {a} {cur} tutarında harcama.",
    "Halkbank: {m} firmasında {a} {cur} tutarlı harcama işlemi gerçekleşmiştir.",
    "VakıfBank: Kartınızla {m} işyerinde {a} {cur} alışveriş yapılmıştır.",
    "TEB: {d} tarihinde {m}'de {a} {cur} tutarında kartlı işlem yapıldı.",
    "Kartınızla {m} isyerinde {a} {cur} harcama yapılmıştır. Bilgilerinize.",
    "{c} kartınızla {m} mağazasında {a} {cur} tutarında harcama gerçekleşti.",
    "Papara: {m}'den {a} {cur} tutarında ödeme yapıldı.",
    "Fibabanka: Kartınız ile {m} üye işyerinde {a} {cur} tutarında harcama işlemi.",
    "İNG: {m} adlı işyerinde {a} {cur} tutarında harcamanız onaylandı.",
    "Şekerbank: {a} {cur} tutarında harcama {m} firmasında gerçekleşti.",
    "Odeabank: Kartınızla {m}'ta {a} {cur} ödeme yapılmıştır.",
    "Anadolubank: {m} işyerinde {a} {cur} tutarında işlem gerçekleşmiştir.",
]

# Isyeri adi verilmeyen bicimler: tutar dogru cikmali, isyeri "-" olmali
NO_MERCHANT_TEMPLATES = [
    "Ziraat: Kartınızla {a} {cur} tutarında harcama yapılmıştır.",
    "Kartınızdan {a} {cur} tutarında ödeme yapılmıştır.",
    "{c} nolu kartınızla {a} {cur} tutarında işlem gerçekleşti.",
]

REFUND_TEMPLATES = [
    "{m} isyerinden {a} {cur} iade yapılmıştır.",
    "Garanti BBVA: {m}'den {a} {cur} tutarında iade hesabınıza geçmiştir.",
    "{m} mağazasından {a} {cur} tutarında iade işlemi gerçekleşti.",
    "İş Bankası: {m} firmasından {a} {cur} iade alındı.",
]

# Islem gibi gorunen ama harcama OLMAYAN metinler -> IGNORE
IGNORE_TEMPLATES = [
    "Tek kullanımlık şifreniz: {otp}. Kimseyle paylaşmayın.",
    "Doğrulama kodunuz {otp}. 3 dakika geçerlidir.",
    "Onay kodu: {otp} - {a} {cur} tutarındaki işleminiz için",
    "İnternetten alışveriş onay kodunuz {otp}, tutar {a} {cur}.",
    "Hesabınızın kullanılabilir bakiyesi {a} {cur}'dir.",
    "Vadesiz hesabınızın bakiyesi {a} {cur} olarak güncellenmiştir.",
    "Kartınızla yapılan {a} {cur} tutarındaki işlem iptal edilmiştir.",
    "{m} işyerinde {a} {cur} tutarındaki provizyon iptal edilmiştir.",
    "{a} {cur} tutarındaki işleminiz başarısız oldu, lütfen tekrar deneyin.",
    "Kampanya! Bu ay {a} {cur} harcamaya {a2} {cur} hediye çeki kazanın.",
    "Fırsat: {m} alışverişlerinde {a} {cur} indirim sizi bekliyor.",
    "Kredi kartı ekstreniz hazır. Son ödeme tarihi {d}, borcunuz {a} {cur}.",
    "Asgari ödeme tutarınız {a} {cur}. Son ödeme tarihi {d}.",
    "Kredi kartınıza tanımlı {a} {cur} tutarındaki otomatik ödeme talimatı işlenmiştir.",
    "Kart limitiniz {a} {cur} olarak güncellenmiştir.",
    "{m} için {a} {cur} tutarında puan kazandınız.",
]

# Hicbir sekilde eslesmemesi gereken metinler -> NONE
NONE_TEMPLATES = [
    "Kargonuz yola çıktı, takip numarası 1234567890.",
    "Siparişiniz hazırlanıyor. Tahmini teslim {d}.",
    "Randevunuz {d} tarihinde saat 14:30'da onaylanmıştır.",
    "Yarın hava sıcaklığı 28 derece, güneşli.",
    "Toplantı hatırlatması: {d} saat 10:00.",
    "{m} mağazamız yeni adresinde hizmetinizde.",
    "Parolanız başarıyla güncellendi.",
    "Yeni bir cihazdan giriş yapıldı. Siz değilseniz bizi arayın.",
]


def tr_amount(minor):
    """24590 -> '245,90'  |  1250000 -> '12.500,00'"""
    lira, kurus = divmod(minor, 100)
    grouped = f"{lira:,}".replace(",", ".")
    return f"{grouped},{kurus:02d}"


def tr_amount_whole(minor):
    """Bazi bankalar tam sayilarda kurus yazmaz: 17500 -> '175'"""
    lira, kurus = divmod(minor, 100)
    if kurus != 0:
        return None
    return f"{lira:,}".replace(",", ".")


rows = []


def emit(text, kind, minor, merchant):
    amount = "-" if minor is None else str(minor)
    # 5. kolon koken: bu uretecin cikardigi her satir tanim geregi SYNTHETIC.
    rows.append((text, kind, amount, merchant, "SYNTHETIC"))


def fill(tpl, m_raw="", a_str="", cur="TL", card=None, date=None, otp=None, a2=None):
    return (tpl
            .replace("{m}", m_raw)
            .replace("{a2}", a2 or "50,00")
            .replace("{a}", a_str)
            .replace("{cur}", cur)
            .replace("{c}", card or random.choice(CARDS))
            .replace("{d}", date or random.choice(DATES))
            .replace("{otp}", otp or str(random.randint(100000, 999999))))


# --- Harcamalar: her sablon x birkac isyeri/tutar kombinasyonu ---
for i, tpl in enumerate(EXPENSE_TEMPLATES):
    for j in range(5):
        m_raw, m_expected = MERCHANTS[(i * 5 + j) % len(MERCHANTS)]
        minor = AMOUNTS[(i * 3 + j) % len(AMOUNTS)]
        # her 7. ornekte kurussuz yazim dene (banka bicim cesitliligi)
        whole = tr_amount_whole(minor) if (i + j) % 7 == 0 else None
        a_str = whole or tr_amount(minor)
        emit(fill(tpl, m_raw, a_str), "EXPENSE", minor, m_expected)

# --- Yabanci para ---
for cur in ("USD", "EUR"):
    for k in range(3):
        m_raw, m_expected = MERCHANTS[(k * 7) % len(MERCHANTS)]
        minor = AMOUNTS[k]
        text = fill(EXPENSE_TEMPLATES[2], m_raw, tr_amount(minor), cur)
        emit(text, "EXPENSE", minor, m_expected)

# --- Isyeri adi olmayanlar ---
for i, tpl in enumerate(NO_MERCHANT_TEMPLATES):
    for j in range(3):
        minor = AMOUNTS[(i * 4 + j) % len(AMOUNTS)]
        emit(fill(tpl, "", tr_amount(minor)), "EXPENSE", minor, "-")

# --- Iadeler ---
for i, tpl in enumerate(REFUND_TEMPLATES):
    for j in range(3):
        m_raw, m_expected = MERCHANTS[(i * 6 + j) % len(MERCHANTS)]
        minor = AMOUNTS[(i * 5 + j) % len(AMOUNTS)]
        emit(fill(tpl, m_raw, tr_amount(minor)), "REFUND", minor, m_expected)

# --- Yok sayilmasi gerekenler ---
for i, tpl in enumerate(IGNORE_TEMPLATES):
    for j in range(2):
        m_raw, _ = MERCHANTS[(i * 3 + j) % len(MERCHANTS)]
        minor = AMOUNTS[(i * 2 + j) % len(AMOUNTS)]
        emit(fill(tpl, m_raw, tr_amount(minor)), "IGNORE", None, "-")

# --- Hic eslesmemesi gerekenler ---
for i, tpl in enumerate(NONE_TEMPLATES):
    m_raw, _ = MERCHANTS[i % len(MERCHANTS)]
    emit(fill(tpl, m_raw), "NONE", None, "-")

out = sys.stdout
out.write("# OTOMATIK URETILDI - scripts/generate_corpus.py\n")
out.write("# Bu metinler gercege benzetilmis SABLONLARDIR, gercek bildirim degildir.\n")
out.write("# Gercek ornekleri ./scripts/add-fixture.sh ile ekleyin.\n")
out.write("# kolonlar: metin <TAB> kind <TAB> amountMinor <TAB> merchant <TAB> origin\n")
out.write("# origin: REAL|SYNTHETIC. Eksikse SYNTHETIC sayilir.\n")
out.write("# Yayin karari YALNIZCA REAL satirlarin dogruluguna bakar.\n")
for r in rows:
    out.write("\t".join(r) + "\n")

print(f"# toplam {len(rows)} ornek", file=sys.stderr)

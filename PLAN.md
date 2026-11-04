# Tasbih Counter — Android ilova rejasi (v1.0)

## Yo'l xaritasi
1. ~~Zikr jumlalari~~ — **2-releasega** ko'chirildi, talablar 9-bo'limda qayd etildi
2. ~~Dizayn uslubini tanlash~~ — **A (Sokin minimal) + Oddiy tasbihda C (donalar)** (8.1)
3. ~~HTML mockup + tuzatishlar~~ — tasdiqlandi (`mockup/index.html`)
4. ~~Texnologiyani yakuniy tanlash~~ — 1-bo'lim
5. ~~Prototip~~ — 1-release kodi yozildi (`app/`), Redmi Note 14 da sinaldi; UI sayqal (pastki menyu, insets)
6. ~~1.1-release qo'shimchalari~~ (11-bo'lim) — v0.2.0, Redmi Note 14 da tekshirildi
7. **Real foydalanish sinovi** (7-bo'lim, 1-qadam): ekran o'chiqda Volume, kombinatsiyalar, qo'ng'iroq/budilnik, proximity, HyperOS ← *hozir*
8. **2-release — zikr jumlalari** (9-bo'lim): avval manba tanlanadi

## 0. Kontekst (qarorlar)
- Shaxsiy foydalanish, Play Market yo'q, APK sideload.
- Qurilma: **Redmi Note 14** (HyperOS, Android 14/15). Barcha test shu qurilmada.
- Barcha kerakli ruxsatlar beriladi (Accessibility ham).
- Sanash paytida ilova ochiq; telefon qo'lda yoki cho'ntakda.
- Power, Volume Up, Volume Down — hammasi **+1**.
- Sessiya boshlanganda boshqa ilovalardagi audio (musiqa, video, podkast) to'xtatiladi —
  faqat **Qurilma tugmalari** rejimida (Oddiy tasbihda tegilmaydi).
- Ekran yoniq paytida Home bilan chiqilsa — avtomatik pauza.
- DND (Bezovta qilmang) — sozlamada bor, default **o'chiq**.
- Maqsadga yetganda — signal beriladi, hisob **to'xtamasdan davom etadi** (0 ga qaytmaydi).
- Telefon jim/vibratsiya rejimida — ovoz o'rniga faqat vibratsiya.
- Chekka tugma — default **o'chiq**, sozlamadan yoqiladi.
- Tarix va statistika saqlanadi.
- Har zikr hisobi sessiyalar orasida **saqlanib davom etadi**; Boshlashdan oldin reset qilish mumkin.
  Reset statistikaga ta'sir qilmaydi (statistika sessiyalarda sanalganlardan hisoblanadi).
- Til: o'zbek (lotin, default) + ingliz.
- Tarixni faylga eksport/import (backup).
- **1-release — faqat counter.** Zikr matnlari, ma'nolari va shu kabilar — **2-release** (9-bo'lim).
  1-releaseda shablon — faqat nom + maqsad (masalan "33", "1000"), diniy matn yo'q.
- **Ikki sanash rejimi** (asosiy oynada switch):
  - **Qurilma tugmalari** (default) — Volume/Power, ekran himoyasi, Accessibility (2–5-bo'limlar).
  - **Oddiy tasbih** — faqat ekrandagi katta tugmaga bosib sanash. Himoya yo'q, tugmalar ushlanmaydi
    (telefon odatdagidek ishlaydi), Accessibility shart emas. Vibratsiyalar (har bosish, oraliq, maqsad),
    ekran o'chmasligi, tarix va statistika — xuddi shunday ishlaydi.

## 1. Texnologiya (yakuniy)
**Native Android: Kotlin + Jetpack Compose.** Flutter/React Native rad etildi: ilovaning asosi —
AccessibilityService, screen pinning, SCREEN_ON/OFF, proximity, show-when-locked, audio fokus —
baribir native kod; ikki qatlam ortiqcha.

| Qism | Tanlov | Izoh |
|---|---|---|
| Til / UI | Kotlin 2.x, Jetpack Compose, Material 3 | bitta Activity |
| minSdk / targetSdk | **31** (Android 12) / 36 | shaxsiy foydalanish: `TelephonyCallback`, `VibratorManager`, audio mode listener, dynamic color — hammasi API 31+, eski versiya shoxlari kerak emas |
| Navigatsiya | Navigation Compose | |
| Holat | ViewModel + StateFlow | |
| Saqlash | **Room** — shablonlar (joriy hisob bilan), sessiyalar; **DataStore** — sozlamalar | hisob har sanashda Room'ga yoziladi |
| Backup | kotlinx.serialization (JSON) + Android fayl tanlagich (SAF) | ruxsat kerak emas |
| Grafik | Compose `Canvas` (oddiy ustunlar) | kutubxonasiz |
| Tillar | `strings.xml` (uz default, `values-en`) + per-app language | |
| DI | yo'q — `Application` ichida qo'lda | bitta modul uchun yetarli |
| Mantiq testlari | JUnit — holat mashinasi, tugma kombinatsiyalari, power filtri (sof Kotlin klasslar) | qurilmaga bog'liq qism — qo'lda, Redmi'da |
| Build | Gradle (Kotlin DSL) + version catalog, wrapper | APK `adb install` orqali |

**Kod tuzilishi (bitta modul):**
```
app/src/main/java/.../
  ui/        — oynalar (Compose), mavzu, navigatsiya
  data/      — Room (entity, dao), DataStore, backup
  engine/    — sof Kotlin: SessionStateMachine, KeyInterpreter (kombinatsiya/debounce), PowerFilter
  service/   — CounterAccessibilityService (tugmalar, ekran ON/OFF, qo'ng'iroq, bildirishnoma)
```
`engine/` Android'ga bog'liq emas — eng nozik mantiq (5-bo'lim) unit-test bilan qoplanadi.

**Muhit (tekshirildi, 2026-11-01):** JDK 17, Android SDK (platform 31–36.1, build-tools 37), adb — bor.
Android Studio va Gradle — yo'q (Studio shart emas; Gradle wrapper yaratish uchun bir marta `brew install gradle` kerak).
- **Bitta `AccessibilityService`**: Volume tugmalarni ushlaydi, `SCREEN_ON/OFF` ni tinglaydi (power),
  bildirishnoma hodisalarini ko'radi, doimiy bildirishnoma ko'rsatadi.
  Zaxira (agar Redmi'da ekran o'chiqda Vol hodisalari kelmasa): MediaSession — 1-bosqich testida aniqlanadi.

## 2. Ilova oqimi

### 2.1. Ochilish — Sozlash ekrani (sessiyadan oldin)
Ilovaga kirilganda birinchi shu ekran. Hamma narsa shu yerda tanlanadi, keyin **Boshlash**.

| Element | Variantlar | Default |
|---|---|---|
| Zikr | ro'yxatdan tanlash / yangi qo'shish (nomini o'zi kiritadi) | oxirgi tanlangan |
| Joriy hisob | tanlangan zikrning oxirgi hisobi ko'rinadi + **Reset** tugmasi (tasdiq bilan) | oxirgi hisobdan davom |
| Sanash rejimi | Qurilma tugmalari / Oddiy tasbih. Oddiy tasbihda quyidagi himoya va tugma qatorlari yashiriladi | Qurilma tugmalari |
| Maqsad (rejim) | 33 / 99 / 100 / 1000 / o'zim kiritaman / **Cheksiz** | 33 |
| Maqsad signali | Vibratsiya / Ovoz / Ikkalasi (Cheksiz rejimda yashirin). Telefon jim bo'lsa — faqat vibratsiya | Vibratsiya |
| Ekran himoyasi | yoqiq / o'chiq (4-bo'lim) | yoqiq |
| Sanash: Volume tugmalar | yoqiq / o'chiq | yoqiq |
| Sanash: Power tugma | yoqiq / o'chiq | yoqiq |

- **Volume va Power ikkalasi bir vaqtda o'chiq bo'lishi mumkin emas.** Bittasi qolganda uning
  switch'i bloklanadi (disabled) va yonida izoh: "Kamida bitta tugma yoqiq bo'lishi kerak".
- Ekranga bosib sanash har doim ishlaydi (himoya o'chiq bo'lsa).
- Pastda **qisqa yo'riqnoma kartasi** (har doim ko'rinadi):
  - "Vol Up + Vol Down birga qisqa bosish — pauza / davom"
  - "Vol Up + Vol Down birga 3 soniya ushlab turish — sessiyani tugatish"
  - "Ekran himoyasini ochish — qulf belgisini 2 soniya bosib turish"
- Birinchi marta (yoki Accessibility o'chiq bo'lsa) — Boshlash o'rniga "Ruxsatlarni sozlash" (6-bo'lim).

### 2.2. Boshlash bosilganda
1. Audio fokus olinadi (`AUDIOFOCUS_GAIN`) — boshqa ilovalardagi musiqa/video to'xtaydi.
2. `FLAG_KEEP_SCREEN_ON`, `setShowWhenLocked`, `setTurnScreenOn`.
3. Ekran himoyasi yoqiq bo'lsa — Lock qatlami + screen pinning.
4. DND sozlamada yoqiq bo'lsa — yoqiladi.
5. Holat → ACTIVE, "boshlandi" vibratsiyasi.
6. Yo'riqnoma 3 soniya ekran tepasida ko'rinib turadi, keyin yo'qoladi.

### 2.3. Sessiya ekrani
- Markazda katta hisob raqami, maqsad progress halqasi, zikr nomi.
  Maqsaddan oshganda halqa to'la qoladi, hisob davom etadi (masalan "41 / 33 ✓").
- **Ekran o'chmaydi** va har doim hisobni ko'rsatadi. Power bilan o'chirilsa — keyingi bosishda
  to'g'ridan-to'g'ri shu ekran (lock-screen ustida) ochiladi.
- Proximity "yaqin" bo'lsa (cho'ntak) — ekran qora, yorug'lik minimal, touch e'tiborsiz.
- **Chekka tugma** (default o'chiq, sozlamadan yoqiladi; himoya yoqiq bo'lsa ham ishlaydi):
  **chap yuqori burchak** — o'ng qo'l bilan bir qo'lda ushlaganda eng kam tegiladigan joy,
  cho'ntakda ham kam ishqalanadi. 1 soniya bosib turish → **Pauza + himoyani o'chirish**.
  Qo'ng'iroq/budilnik paytida kattalashib, ko'zga tashlanadi (5.3).
- Himoya o'chiq bo'lsa: undo (−1), reset (tasdiq bilan), pauza, tugatish tugmalari ko'rinadi.
  Sessiya ichida reset: joriy sessiya tarixga yoziladi, hisob 0 dan, yangi sessiya yozuvi ochiladi.

### 2.4. Tugatish
Vol Up+Down 3s / ekrandagi "Tugatish" / bildirishnomadagi "To'xtatish" →
audio fokus qaytariladi, pinning va KEEP_SCREEN_ON olinadi, DND avvalgi holatga, holat → IDLE,
sessiya tarixga yoziladi, Sozlash ekraniga qaytish.

## 3. Sessiya holat mashinasi
Tugmalar faqat **ACTIVE** holatda sanaydi va ushlanadi. Boshqa holatlarda telefon odatdagidek ishlaydi.

```
IDLE ──[Boshlash]──> ACTIVE ──[pauza]──> PAUSED ──[davom]──> ACTIVE
                       │
                       ├──[qo'ng'iroq / budilnik]──> AUTO_PAUSED ──[tugadi + 2s]──> ACTIVE
                       └──[tugatish]──> IDLE
```
- `PAUSED` — foydalanuvchi to'xtatgan; faqat o'zi davom ettiradi.
- `AUTO_PAUSED` — tizim to'xtatgan; sabab tugagach avtomatik davom etadi va himoya avvalgi holatiga qaytadi.
- AUTO_PAUSED paytida chekka panel yoki Vol kombinatsiya bosilsa → PAUSED (avto-davom yo'q).
- PAUSED paytida ekran himoyasi o'chiq bo'ladi; davom bosilganda sozlamaga qarab qayta yoqiladi.

## 4. Ekran himoyasi
| Xavf | Yechim |
|---|---|
| Ekrandagi boshqa narsalarni bosish | Lock qatlami: faqat hisob va chekka panel ko'rinadi. Ochish — qulf belgisini 2s bosib turish |
| Back ishorasi | `BackHandler` yutadi |
| Home / Recents / bildirishnoma paneli | Screen pinning (`startLockTask`). Telefonda "Ilovani mahkamlash" yoqilgan bo'lishi kerak |
| Cho'ntakda tegish | Proximity "yaqin" → touch e'tiborsiz |
| Ekran o'zi o'chishi | `FLAG_KEEP_SCREEN_ON` |
| Power bilan yoqilganda lock-screen | `setShowWhenLocked(true)` + `setTurnScreenOn(true)` |
| Qo'ng'iroq UI pinning tufayli to'silishi | Qo'ng'iroq/budilnik aniqlanganda pinning va Lock qatlami **avtomatik olinadi** (5.3) |

## 5. Holatlar matritsasi

### 5.1. Tugma bosish turlari
| Holat | Xatti-harakat |
|---|---|
| Bitta qisqa bosish (Vol) | +1. Sanash **ACTION_UP** da (kombinatsiyani ajratish uchun) |
| Tez ketma-ket ikki bosish | Ikkalasi ham +1. Minimal oraliq 80ms (kontakt sakrashidan himoya) |
| Vol uzoq bosib turish | Auto-repeat (`repeatCount > 0`) e'tiborsiz → 1 marta sanaladi |
| Vol Up + Vol Down birga, qisqa | **Pauza ↔ Davom.** Sanalmaydi |
| Vol Up + Vol Down birga, 3s | **Sessiyani tugatish.** 1.5s da ogohlantiruvchi vibratsiya ("qo'yib yuborsangiz bekor"). Tizim accessibility shortcut'i o'chirilgan bo'lishi kerak |
| Volume sanash sozlamada o'chiq | Bitta Vol bosishlar ushlanmaydi (tizim ovozi o'zgaradi); **kombinatsiyalar baribir ishlaydi** |
| Power bitta bosish | +1 (5.2) |
| Power ikki marta tez | HyperOS kamera → sozlamada o'chirish shart. Keyin har biri +1; oraliq < 250ms bo'lsa bittasi tashlanadi |
| Power uzoq bosish | Ekran holati o'zgarmaydi → sanalmaydi |
| Power + Vol (screenshot) | Power hodisasi paytida Vol bosilgan bo'lsa — hech biri sanalmaydi |
| Vol Down ikki marta (ekran o'chiq) | HyperOS Quick snap → sozlamada o'chirish shart |

### 5.2. Power qanday sanaladi
Accessibility servisi `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF` ni tinglaydi; **har o'tish = +1**.
| Soxta manba | Filtr |
|---|---|
| Ekran timeout | KEEP_SCREEN_ON tufayli bo'lmaydi |
| Bildirishnoma ekranni yoqishi | `TYPE_NOTIFICATION_STATE_CHANGED` dan keyin 1.5s ichidagi SCREEN_ON sanalmaydi + HyperOS'da "bildirishnomada ekranni yoqish" o'chiriladi |
| Proximity (quloq, cho'ntak) | Qo'ng'iroqda AUTO_PAUSED; Pocket mode testda tekshiriladi |
| Budilnik ekranni yoqishi | AUTO_PAUSED |
| < 250ms o'tish | Tashlanadi |

ACTIVE da power bilan qulflash = +1. Sanamasdan qulflash uchun avval pauza.

### 5.3. Tashqi hodisalar
| Hodisa | Xatti-harakat |
|---|---|
| Kiruvchi / chiquvchi qo'ng'iroq (GSM, Telegram, WhatsApp) | `AudioManager` mode (`RINGTONE` / `IN_CALL` / `IN_COMMUNICATION`) + `TelephonyCallback` → **AUTO_PAUSED**, pinning va Lock qatlami olinadi, Vol tugmalar ushlanmaydi (ringtone o'chirish ishlaydi), chekka tugma (yoqiq bo'lsa) kattalashadi. Qo'ng'iroq tugagach 2s → ACTIVE, himoya qaytadi |
| Budilnik / taymer | Audio fokusni vaqtincha yo'qotish (`AUDIOFOCUS_LOSS_TRANSIENT`) + alarm stream faolligi → yuqoridagidek AUTO_PAUSED. Aniqlanmasa — chekka panel orqali qo'lda |
| Musiqa / video | Boshlashda audio fokus orqali to'xtatiladi. Sessiya paytida qayta yoqilsa ham Vol tugmalar sanaydi (ovoz o'zgarmaydi) |
| Bildirishnoma | Sanash davom etadi; 5.2 filtri ishlaydi |
| Ekran yoniq paytida Home / boshqa ilovaga o'tish (himoya o'chiq) | `onStop` + ekran yoniq → **PAUSED** |
| Ekran o'chishidan `onStop` | Sessiya davom etadi |
| Jarayon o'ldirildi (MIUI) | Hisob saqlangan; ilova ochilganda Accessibility holatini tekshirib ogohlantiradi |
| Telefon qayta yoqildi | IDLE, oxirgi hisob saqlangan |

### 5.4. Signal naqshlari
| Hodisa | Vibratsiya | Ovoz (agar tanlangan) |
|---|---|---|
| +1 | 1 × qisqa (20ms) | — |
| Oraliq qadam (har 33, sozlanadi) | 2 × qisqa | — |
| Maqsadga yetdi (hisob davom etadi) | 1 × uzun (400ms) | qisqa signal (jim rejimda yo'q) |
| Boshlandi / Davom | 1 × o'rtacha | — |
| Pauza | 2 × o'rtacha | — |
| Tugatish ogohlantirishi (1.5s) | 1 × qisqa | — |
| Sessiya tugadi | 3 × qisqa | — |
| AUTO_PAUSED | yo'q | — |

## 6. Umumiy sozlamalar (Sozlash ekranidan tashqari, alohida sahifa)
- Har sanashda vibratsiya: yoqiq/o'chiq, kuchi
- Oraliq qadam signali (barcha rejimlarda, Cheksiz ham): o'chiq / har 33 / o'zim kiritaman — default **har 33**
- Til: O'zbekcha / English — default O'zbekcha
- Backup: tarix, zikrlar va sozlamalarni JSON faylga eksport; fayldan import (Android fayl tanlagich orqali, ruxsatsiz)
- DND sessiya paytida — default **o'chiq**
- Chekka tugma — default **o'chiq**
- Mavzu: tungi / kunduzgi / tizim
- Zikrlar ro'yxatini boshqarish
- Tarixni tozalash (tasdiq bilan)

## 6.1. Tarix va statistika
**Saqlanadigan yozuv (har sessiya):** zikr, boshlanish va tugash vaqti, sof davomiylik (pauzasiz),
**sessiyada sanalgan son** (undo hisobga olingan), boshlang'ich va yakuniy hisob, maqsad, maqsadga yetildimi,
sanash manbalari (ekran / Volume / Power — nechtadan).
Statistika faqat "sessiyada sanalgan son"dan hisoblanadi — shuning uchun Reset uni buzmaydi.
Yarim tunni kesib o'tgan sessiya — boshlangan kunga yoziladi (ponytail: soddalik uchun; kerak bo'lsa bo'linadi).
Jarayon o'lsa ham yo'qolmasligi uchun sessiya boshlanishida yozuv ochiladi va har o'zgarishda yangilanadi.

**Statistika (taklif):**
- Bugun / hafta / oy / jami — umumiy hisob
- Zikr bo'yicha taqsimot
- Kunlik grafik (oxirgi 7 / 30 kun)
- Ketma-ket kunlar (streak)
- Sessiyalar ro'yxati (tafsilot bilan)

### Telefon sozlamalari (bir martalik, ilova ichida yo'riqnoma)
- Accessibility → ilova servisini yoqish
- Batareya → "Cheklovsiz"; Avtostart → yoqish; Recents'da ilovani qulflash
- Screen pinning → yoqish
- Power ikki marta → kamera: o'chirish; Vol Down ikki marta (Quick snap): o'chirish
- Accessibility shortcut (Vol Up+Down): o'chirish
- Bildirishnomada ekranni yoqish: o'chirish
- Ruxsatlar: Notifications, Phone state, (DND yoqilsa) Notification policy

## 7. Bosqichlar
1. **Prototip sinovi (Redmi Note 14)**: ekran o'chiqda Accessibility Vol hodisalarini oladimi; SCREEN_ON/OFF ishonchliligi; Vol Up+Down kombinatsiyani tizim ushlab qolmaydimi; pinning paytida qo'ng'iroq UI; Pocket mode; budilnik aniqlash. Natijaga qarab 5-bo'lim tuzatiladi
2. Sozlash ekrani + sessiya ekrani: bosib sanash, maqsad/cheksiz, signal, undo/reset, saqlash, tarix yozuvi
3. Accessibility servisi: Vol tugmalar, kombinatsiyalar, holat mashinasi, bildirishnoma, audio fokus
4. Power sanash + filtrlar
5. Tashqi hodisalar (qo'ng'iroq, budilnik) + chekka panel
6. Himoya: Lock qatlami, pinning, proximity, show-when-locked
7. Zikrlar ro'yxati, umumiy sozlamalar, telefon sozlash yo'riqnomasi
8. Tarix va statistika ekranlari
9. Til (uz/en), backup eksport/import
10. Real foydalanish testi va sayqal

## 8. Ekranlar va dizayn (taklif, muhokamada)

### 8.1. Uslub
**Tanlangan:** A — Sokin minimal (butun ilova; sanash oynasida zumrad progress halqa).
Oddiy tasbih rejimida — C: maqsad ≤ 100 bo'lsa halqa o'rniga **donalar** (har sanash = bitta dona to'ladi);
maqsad > 100 yoki Cheksiz bo'lsa — halqa.

- **Sokin minimalizm**: ortiqcha bezaksiz, bitta urg'u rangi, ko'p bo'sh joy,
  katta raqamlar (tabular shrift — raqam o'zgarganda "sakramaydi").
- **Rang**: asosiy urg'u — **Zumrad yashil** (islomiy ilovalarda an'anaviy, ko'zni charchatmaydi,
  qora fonda yaxshi o'qiladi). Ikkinchi urg'u — **yumshoq oltin**, faqat "maqsadga yetildi" holati uchun.
  | Token | Qorong'i | Kunduzgi |
  |---|---|---|
  | Fon | `#000000` (sanash), `#0E1412` (boshqa oynalar) | `#F7F8F6` |
  | Sirt (karta) | `#18201D` | `#FFFFFF` |
  | Urg'u (zumrad) | `#3DBB8E` | `#1E7A5C` |
  | Oltin (maqsad) | `#D4B25A` | `#A8862E` |
  | Asosiy matn | `#E8EDEA` | `#14201B` |
  | Ikkinchi matn | `#93A39C` | `#5A6B64` |
  Kontrast WCAG AA dan past bo'lmasligi implementatsiyada tekshiriladi.
- **Rangni sozlash** (Sozlamalar → Ko'rinish): Zumrad (default) / Oltin / Moviy / Binafsha /
  **Tizim** (Material You — devor qog'ozidan, Android 12+). Oltin "maqsad" rangi har doim saqlanadi.
- **Qorong'i mavzu birinchi o'rinda** — zikr ko'pincha kechqurun/tunda; Redmi Note 14 AMOLED ekranida
  qora fon batareyani tejaydi (ekran uzoq yoniq turadi). Kunduzgi mavzu ham bor.
- **Haptic-first**: har muhim amal o'z vibratsiyasiga ega (5.4) — ekranga qaramasdan tushuniladi.
- **Bir qo'l bilan**: asosiy tugmalar ekranning pastki yarmida (bosh barmoq yetadigan joyda);
  xavfli amallar (reset, tugatish) — yuqorida yoki tasdiq bilan.
- Material 3 komponentlari, animatsiya minimal (faqat holat o'zgarishida qisqa).

### 8.2. Navigatsiya
Pastki panel, 3 bo'lim: **Asosiy · Statistika · Sozlamalar**.
Kirish (onboarding) — faqat birinchi marta. Sanash oynasi — to'liq ekran, panelsiz.
Export/Import — alohida oyna, Sozlamalar → "Ma'lumotlar" orqali kiriladi (tez-tez ishlatilmaydi, panelga chiqarish shart emas).

```
Kirish (1 marta) ──> Asosiy ──[Boshlash]──> Sanash ──[Tugatish]──> Asosiy (+ qisqa natija)
                      │  ▲
          Statistika ─┘  └─ Sozlamalar ──> Export/Import
                                      └──> Ruxsatlar (Kirishdagi qadamlar qayta)
```

### 8.3. "Shablon" tushunchasi
Shablon = nom + **bir qismdagi son** + **qismlar soni** + sanash rejimi + maqsad signali + o'z joriy hisobi.
- Maqsad = son × qismlar. Masalan `33 × 3` — jami 99, har 33 da oraliq signal; `99` — bir martada 99.
- Qismli shablonda oraliq signal qism chegarasida (umumiy "oraliq signal" sozlamasi o'rniga).
- Nom bo'sh qolsa — avtomatik (`33 × 3`, `1000`, `Cheksiz`).
Kirishda neytral default shablonlar: **33, 33 × 3, 99, 100, 1000, Cheksiz**.
Diniy matnli tayyor ro'yxat — keyinroq (9-bo'lim).

### 8.4. Oynalar

**1) Kirish (onboarding)** — 4 qadam, yuqorida progress nuqtalari, har qadam "O'tkazib yuborish" mumkin:
1. Til tanlash (O'zbekcha / English) + qisqa tanishtiruv (1 jumla)
2. Qanday ishlaydi: 3 kichik karta — "Volume/Power bilan sanash", "Vol Up+Down — pauza", "3s ushlab — tugatish"
3. Ruxsatlar: ro'yxat, har birida holat (✓ / ✗) va "Ochish" tugmasi (to'g'ridan tizim sahifasiga):
   Accessibility, Bildirishnomalar, Telefon holati, Batareya "Cheklovsiz", Screen pinning.
   HyperOS qo'lda sozlanadiganlar (kamera, Quick snap, shortcut, bildirishnomada ekran yonishi) —
   belgilanadigan checklist ("bajardim").
4. Shablonlar: default 5 ta, belgilab olib tashlash/qo'shish/nomini o'zgartirish.
- Ruxsat berilmasa ham ilova ishlaydi — faqat "Oddiy tasbih" rejimi ochiq bo'ladi.

**2) Asosiy oyna**
- Yuqorida: **Bugun kartasi** — bugungi jami (o'rtacha-katta raqam), kecha bilan solishtirish
  ("kechagidan +180" yoki "kechagiga yetish uchun 260 qoldi"), o'ng tomonda streak (olov + kunlar soni).
  Bosilsa Statistikaga o'tadi.
- Shablonlar — **2 ustunli kartalar**: nom, rejim belgisi (qurilma / qo'l), joriy hisob (katta),
  pastida: `/ maqsad` yoki qismli bo'lsa `2-qism · 8/33`, maqsadga yetilsa oltin ✓.
  Progress chizig'i olib tashlandi (hisob maqsaddan oshib davom etgani uchun ma'nosini yo'qotadi).
  Kartani bosish → **Tayyorlanish varag'i** (bottom sheet) ochiladi:
  - joriy hisob katta raqamda + Reset (tasdiq bilan)
  - **Sanash rejimi switch**: Qurilma tugmalari (default) / Oddiy tasbih
  - Qurilma rejimida: Ekran himoyasi, Volume, Power switch'lari (kamida bittasi yoqiq), yo'riqnoma kartasi
  - Maqsad signali (vibratsiya / ovoz / ikkalasi)
  - pastda katta **Boshlash** tugmasi
- **Tanlovlarni eslab qolish (qaror):**
  - *Har shablon uchun*: sanash rejimi (Qurilma / Oddiy) va maqsad signali — chunki rejim vaziyatga bog'liq
    (masalan "1000" — yo'lda cho'ntakda, "33" — namozdan keyin qo'lda). Shablonning bir qismi.
  - *Umumiy (oxirgi ishlatilgan)*: ekran himoyasi, Volume, Power — bular qurilma odati, shablonga bog'liq emas.
  - Natija: odatda karta → Boshlash (2 bosish), qayta sozlash kam kerak bo'ladi.
- "+" — **Yangi shablon oynasi**: yuqorida jonli ko'rinish (`33 × 3`, "jami 99 · har 33 da signal"),
  nom (ixtiyoriy), bir qismdagi son (33 / 99 / 100 / 1000 / ∞ yoki o'zim kiritaman),
  qismlar soni (− / +, 1–10; ∞ da yashirin), sanash rejimi, maqsad signali, Saqlash.
- **Shablonlarni boshqarish — ikki yo'l, bitta tahrirlash oynasi:**
  - *Asosiy yo'l*: Sozlamalar → **Shablonlar** — barcha shablonlar ro'yxati: sudrab tartiblash
    (bosh sahifadagi tartib), har qatorda Tahrirlash va O'chirish, "+" bilan qo'shish.
  - *Tez yo'l*: bosh sahifada kartani uzoq bosish → menyu: Tahrirlash / Hisobni reset / O'chirish.
  - Tahrirlash — "Yangi shablon" oynasining o'zi (to'ldirilgan holda). O'chirish — tasdiq oynasi
    ("Tarix va statistika saqlanib qoladi"). Default shablonlar ham tahrirlanadi/o'chiriladi.
- Ruxsat yetishmasa — tepada sariq banner "Qurilma tugmalari uchun ruxsat kerak → Sozlash".

**3) Sanash oynasi** — ikki ko'rinish:
- *Qurilma tugmalari*: qora fon, markazda juda katta raqam, atrofida progress halqa, tepada shablon nomi
  va holat belgisi (▶ / ⏸ / 📞). Himoya yoqiq — qulf belgisi pastda (2s bosib ochiladi).
  Chekka tugma (yoqiq bo'lsa) — chap yuqorida. Boshida 3s yo'riqnoma.
  Pauza holatida — raqam xiralashadi, "Davom" va "Tugatish" tugmalari chiqadi.
- *Oddiy tasbih*: raqam yuqori yarmida, pastki yarmi — **katta yumaloq bosish maydoni** (bosh barmoq joyi),
  bosganda kichik "bosilish" animatsiyasi + vibratsiya. Kichik tugmalar: undo (−1), pauza, tugatish.
- Tugatgach — qisqa natija (bottom sheet): "+127 · 8 daqiqa · maqsadga yetildi ✓".

**4) Statistika**
- Yuqorida davr tanlash: Bugun / Hafta / Oy / Jami.
- Katta raqam — davr jami; yonida streak (🔥 N kun).
- Ustunli grafik (7 yoki 30 kun).
- Shablonlar bo'yicha taqsimot (ro'yxat + ulush chizig'i).
- Sessiyalar ro'yxati (sana bo'yicha guruhlangan) → bosilsa tafsilot: vaqt, davomiylik, manbalar (ekran/Vol/Power).

**5) Sozlamalar** — bo'limlar:
0. **Shablonlar** (eng yuqorida) → Shablonlar oynasi
1. **Sanash**: har sanashda vibratsiya + kuchi, oraliq signal (default har 33), default maqsad signali
2. **Qurilma tugmalari**: Volume, Power default holati, kombinatsiyalar haqida ma'lumot, chekka tugma (default o'chiq)
3. **Himoya**: default ekran himoyasi, cho'ntak rejimi (proximity), lock-screen ustida ko'rsatish
4. **Bildirishnoma va DND**: DND sessiya paytida (default o'chiq)
5. **Ko'rinish**: mavzu (qorong'i/kunduzgi/tizim), urg'u rangi (Zumrad/Oltin/Moviy/Binafsha/Tizim), til
6. **Ma'lumotlar**: Export/Import oynasi, tarixni tozalash
7. **Ruxsatlar va telefon sozlamalari**: holatlar + Kirishdagi 3-qadamga o'tish
8. **Ilova haqida**: versiya

**6) Export / Import**
- Oxirgi backup sanasi.
- **Eksport**: tugma → fayl saqlash oynasi, nom `tasbih-backup-YYYY-MM-DD.json`.
- **Import**: fayl tanlash → ko'rib chiqish ("12 shablon, 340 sessiya, 2025-01-03 dan") → rejim tanlash → tasdiq.
  - **Birlashtirish** (default): shablon va sessiyalar ID (UUID) bo'yicha qo'shiladi, takrorlar o'tkazib yuboriladi;
    joriy sozlamalar saqlanadi. Bir xil ID li shablonda — fayldagi versiya joriy hisobni o'zgartirmaydi.
  - **Almashtirish**: hammasi fayldagiga almashtiriladi. Ogohlantirish + undan oldin joriy ma'lumot
    avtomatik zaxiraga eksport qilinadi (qaytarish imkoni bo'lsin).
- Xato fayl — tushunarli xabar, hech narsa o'zgarmaydi.

### 8.5. Dizayn qarorlari (yopildi)
- Oddiy tasbihda musiqa to'xtatilmaydi.
- Rejim + signal — har shablon uchun; himoya/Volume/Power — umumiy.
- Zumrad yashil + oltin urg'u; sozlamada 5 variant.
- Import: Birlashtirish (default) / Almashtirish.
- Bugungi jami — asosiy oynada o'rtacha o'lchamdagi bitta qator.

## 9. Zikr jumlalari — 2-release (1-releasega kirmaydi)
Sabab: katta mas'uliyat, xato va nuqsonga yo'l qo'yib bo'lmaydi, ko'p vaqt oladi — 1-releaseni kechiktirmaslik uchun.
1-releaseda ma'lumotlar modeli shunday quriladi-ki, keyin zikr qo'shish migratsiyasiz bo'lsin
(shablonda bo'sh `zikrId` maydoni). Boshqa tayyorgarlik qilinmaydi.

### 9.1. Tamoyillar
1. **AI hech qanday zikr matni yozmaydi — qoralama ham.** Matn faqat tanlangan va tasdiqlangan ishonchli
   manbadan ko'chiriladi, bir necha marta, bir necha usulda tekshiriladi (9.3), shundan keyin joriy qilinadi.
2. **Manba ko'rsatiladi** (kitob, bob/raqam; Qur'oniy — sura:oyat).
3. **Fazilatlar (savob haqidagi da'volar) yo'q.** Keyin qo'shilsa — faqat aniq manba va tasdiq bilan.
4. **Foydalanuvchi zikr matnini kiritmaydi va o'zgartirmaydi.** Zikrlar faqat development'da qo'shiladi;
   qo'shish/tuzatish ham faqat development'da, yangi versiya orqali. Foydalanuvchi faqat tanlaydi.
5. **Arab yozuvi — hozircha yo'q** (keyingi releaselarda bo'lishi mumkin).
6. **Saqlash formati — buzilmaslik kafolati:**
   - Matnlar alohida fayl (`zikrs.json`), UTF-8, Unicode NFC — kod bilan aralashmaydi, git'da tarix qoladi.
   - Belgilar o'zgarmasligi shart: o'zbek tutuq belgisi (`ʻ` U+02BB, `ʼ` U+02BC) oddiy `'` ga almashmasin,
     kirill harflari (`ў`, `қ`, `ғ`, `ҳ`) lotincha o'xshash harflarga almashmasin.
   - Faylga formatlovchi/linter tegmaydi (`.editorconfig`/istisno).
   - Har yozuvning hash'i (SHA-256) testda qotiriladi — matn tasodifan o'zgarsa test yiqiladi;
     ataylab o'zgartirilsa hash ham ataylab yangilanadi.
   - Ilova faylni faqat o'qiydi, hech qachon qayta yozmaydi.
7. **Ikki yozuv: lotin va kirill** — ikkalasi ham **manbadan** olinadi (avtomatik transliteratsiya yo'q).
   Sozlamada tanlanadi: Lotin (default) / Кирилл.

### 9.2. Har zikr uchun maydonlar
| Maydon | Izoh |
|---|---|
| `id` | o'zgarmas identifikator |
| O'qilishi — lotin | manbadan |
| O'qilishi — kirill | manbadan |
| O'qilishi — en | manbadan (ingliz interfeysi uchun) |
| Ma'nosi — uz lotin / uz kirill / en | manbadan, tarjima manbasi alohida ko'rsatiladi |
| Default maqsad | manbaga ko'ra |
| Toifa | Namozdan keyin / Umumiy / Istig'for / Salovat / Qur'oniy duolar |
| Manba | kitob, bob/raqam yoki sura:oyat |
| Hash | yaxlitlik tekshiruvi uchun |

### 9.3. Kiritish va tekshirish jarayoni (har zikr uchun)
1. Manba tanlanadi va tasdiqlanadi (kim tasdiqlashi — kiritish vaqtida aniqlanadi).
2. Matn manbadan raqamli nusxa sifatida ko'chiriladi (qo'lda terilmaydi).
3. Tekshiruv 1 — manba bilan harfma-harf solishtirish (dastur yordamida, diff).
4. Tekshiruv 2 — boshqa odam (yoki boshqa vaqtda) bosma/asl manba bilan ko'z bilan solishtiradi.
5. Tekshiruv 3 — Redmi'da ilova ichida ko'rinish tekshiriladi (lotin, kirill, en; belgilar to'g'ri chiqqani).
6. Hash qotiriladi, commit, keyin release.

### 9.4. Nomzodlar ro'yxati (qoralama — faqat nomlar; matn, maqsad va kirishi kiritish vaqtida to'liq tekshiriladi)
| Toifa | Zikr (o'qilishi) | Taxminiy maqsad |
|---|---|---|
| Namozdan keyin | Subhanalloh | 33 |
| Namozdan keyin | Alhamdulillah | 33 |
| Namozdan keyin | Allohu akbar | 33 / 34 (manbaga ko'ra) |
| Namozdan keyin | La ilaha illallohu vahdahu la sharika lah... (100 ni to'ldiruvchi) | 1 |
| Umumiy | La ilaha illalloh | 100 |
| Umumiy | Subhanallohi va bihamdihi | 100 |
| Umumiy | Subhanallohi va bihamdihi, subhanallohil 'azim | 100 |
| Umumiy | La havla va la quvvata illa billah | 100 |
| Istig'for | Astag'firulloh | 100 |
| Istig'for | Astag'firullohal 'azim va atubu ilayh | 100 |
| Salovat | Allohumma solli 'ala Muhammad... | 100 |
| Qur'oniy | Hasbunallohu va ni'mal vakil (Oli Imron 173) | 100 |
| Qur'oniy | La ilaha illa anta subhanaka inni kuntu minaz-zolimin (Anbiyo 87) | 100 |

Manba: **ochiq** — kiritish vaqtida aniqlanadi.

### 9.5. Ilovada qanday ko'rinadi (2-release)
- "+" oynasida **"Tayyor zikrlar"** bo'limi — toifalar bo'yicha; foydalanuvchi faqat tanlaydi, shablon yaratiladi.
  Neytral shablonlar (33, 99, 100, 1000, Cheksiz) qoladi.
- Ma'no va manba — shablonni bosib turganda "Ma'lumot" varag'ida.
- **Sanash oynasida zikr matni/ma'nosi** — sozlama, default **o'chiq**. Qurilma tugmalari rejimida (cho'ntak)
  faqat raqam ko'rinadi; yoqilsa ham faqat **Oddiy tasbih** rejimida ko'rsatiladi.
- **Ketma-ketlik (zanjir)** — sozlama: masalan namozdan keyingi tasbihot ketma-ket, maqsadga yetganda keyingisiga
  avtomatik o'tish (alohida vibratsiya bilan).
- Yozuv: Lotin (default) / Кирилл — sozlamada.

## 10. Keyinroq
- Fazilatlar (manba bilan) — tasdiqlangan holda.

## 11. 1.1-release qo'shimchalari

### 11.1. Kunlik maqsad va eslatma
- Sozlamalar → "Kunlik maqsad": son (100 / 300 / 500 / 1000 / boshqa), default **o'chiq**.
- Asosiy → "Bugun" kartasida maqsad progressi ("120 / 300"), bajarilganda ✓.
- Sessiya natijasida bugungi maqsad shu sessiyada bajarilgan bo'lsa — "Kunlik maqsad bajarildi" qatori.
- Eslatma: vaqt tanlanadi (default o'chiq). Shu vaqtda bildirishnoma — faqat maqsad hali bajarilmagan bo'lsa
  (maqsad o'chiq bo'lsa — bugun hali sanalmagan bo'lsa). Qayta yuklashdan keyin ham ishlaydi.
- AlarmManager, aniq bo'lmagan (exact alarm ruxsati kerak emas), bir necha daqiqa kechikishi mumkin.

### 11.2. Bosh ekran vidjeti
- Bugungi son, maqsad progressi yoki seriya (🔥 N kun); yorug'/qorong'i mavzuga moslashadi.
- Vidjetni bosish — ilovani ochadi; ▶ tugmasi — oxirgi ishlatilgan shablonning Tayyorlanish varag'ini ochadi.
- Sessiya davomida va tugaganda yangilanadi.

### 11.3. Quick Settings tugmasi
- Bildirishnoma pardasida "Tasbih" tugmasi; ostida bugungi son.
- Bosilganda — ilova ochilib, oxirgi shablonning Tayyorlanish varag'i chiqadi (▶ bilan bir xil).

### 11.4. Kengaytirilgan statistika
- Yillik kalendar (so'nggi 53 hafta, har kun — rang intensivligi), gorizontal suriladi.
- Tanlangan davr bo'yicha: kunlik o'rtacha, eng yaxshi kun, eng uzun seriya.

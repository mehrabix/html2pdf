[English README is available here.](README.en.md)

# راهنمای استفاده از سرویس HTML to PDF

این سرویس امکان تبدیل کدهای HTML به فایل PDF را با تنظیمات گسترده فراهم می‌کند. شما می‌توانید درخواست خود را به صورت همزمان (sync) برای دریافت آنی فایل، یا غیرهمزمان (async) برای کارهای سنگین و زمان‌بندی شده ارسال کنید.

## اندپوینت‌ها

- `POST /api/pdf/sync`: برای تبدیل فوری و دریافت آنی فایل PDF.
- `POST /api/pdf/async`: برای ثبت یک کار تبدیل، دریافت شناسه و بررسی وضعیت در آینده.
- `GET /api/pdf/job/{id}/status`: برای مشاهده وضعیت یک کار غیرهمزمان.
- `GET /api/pdf/job/{id}/result`: برای دانلود فایل PDF یک کار تکمیل شده (پس از دانلود، کار حذف می‌شود).

## ساختار کامل درخواست (Body)

برای ارسال درخواست، از ساختار JSON زیر استفاده کنید. تمامی فیلدها اختیاری هستند به جز `htmlPages`.

```json
{
  "htmlPages": [
    "<html><head></head><body><h1>سلام دنیا!</h1><h2>بخش اول</h2><p>این یک صفحه تست راست به چپ است.</p><h2>بخش دوم</h2><p>ادامه تست...</p></body></html>"
  ],
  "globalOptions": {
    "--page-size": "A4",
    "--orientation": "Portrait",
    "--margin-top": "20mm",
    "--margin-bottom": "20mm"
  },
  "pageOptions": [
    {}
  ],
  "scheduledTime": "2025-12-31T23:59:59Z",
  "rtl": true,
  "headerHtml": "<html><head><meta charset='UTF-8'></head><body><div style='text-align:center; font-family:Vazirmatn,tahoma; font-size:10pt;'>سربرگ سفارشی</div></body></html>",
  "footerHtml": "<html><head><meta charset='UTF-8'></head><body style='font-family:Vazirmatn,tahoma; font-size:10pt; text-align:center;'>پاصفحه سفارشی</body></html>",
  "addPageNumbering": true,
  "addToc": true,
  "outline": true,
  "dumpOutline": "outline.xml",
  "grayscale": false,
  "lowQuality": false,
  "logLevel": "info",
  "copies": 1,
  "title": "سند تست فارسی",
  "printMediaType": true
}
```

### توضیح کامل فیلدها

- `htmlPages` **(ضروری)**: آرایه‌ای از رشته‌ها که هر رشته، محتوای HTML یک صفحه از PDF است.
- `globalOptions`: یک آبجکت شامل تنظیمات سراسری `wkhtmltopdf`. این تنظیمات روی تمام صفحات اعمال می‌شود. برای مشاهده لیست کامل گزینه‌ها به [مستندات wkhtmltopdf](https://wkhtmltopdf.org/usage/wkhtmltopdf.txt) مراجعه کنید.
- `pageOptions`: آرایه‌ای از آبجکت‌ها برای اعمال تنظیمات اختصاصی به هر صفحه.
- `scheduledTime`: (فقط برای حالت async) زمانبندی اجرای کار در آینده با فرمت `ISO-8601`. مثال: `"2025-07-08T15:00:00Z"`.
- `rtl`: اگر `true` باشد، فونت سفارشی وزیرمتن برای زبان فارسی بارگذاری شده و جهت کلی سند راست به چپ (`rtl`) تنظیم می‌شود. پیش‌فرض `true` است.
- `headerHtml`: کد HTML کامل برای سربرگ (header) تمامی صفحات.
- `footerHtml`: کد HTML کامل برای پاصفحه (footer). **توجه:** این فیلد تنها زمانی استفاده می‌شود که `addPageNumbering` برابر `false` باشد.
- `addPageNumbering`: اگر `true` باشد، شماره‌گذاری خودکار صفحات ("صفحه X از Y") به پاصفحه اضافه می‌شود. این گزینه برای نمایش قطعی شماره صفحات توصیه می‌شود و بر `footerHtml` اولویت دارد.
- `addToc`: اگر `true`


## استفاده از Docker

این پروژه دارای یک Dockerfile آماده است که تمام وابستگی‌های لازم برای اجرای سرویس HTML to PDF را نصب می‌کند، از جمله:

- جاوا (eclipse-temurin:23-jre)
- کتابخانه‌ها و فونت‌های مورد نیاز برای wkhtmltopdf و پشتیبانی از زبان فارسی/راست به چپ
- نصب نسخه خاصی از wkhtmltopdf و libjpeg-turbo
- کپی فونت سفارشی وزیرمتن برای پشتیبانی بهتر از متون فارسی
- کپی فایل jar ساخته‌شده به داخل ایمیج

### مراحل ساخت و اجرای سرویس با Docker

1. **ساخت فایل jar** (در صورت نیاز):
   
   ابتدا مطمئن شوید که فایل jar پروژه ساخته شده است:
   
   ```sh
   ./mvnw clean package
   ```
   یا
   ```sh
   mvn clean package
   ```
   
   فایل jar باید در مسیر `target/html2pdf-0.0.1-SNAPSHOT.jar` قرار بگیرد.

2. **ساخت ایمیج Docker:**
   
   در ریشه پروژه دستور زیر را اجرا کنید:
   
   ```sh
   docker build -t html2pdf-service .
   ```

3. **اجرای سرویس:**
   
   ```sh
   docker run -p 3000:3000 html2pdf-service
   ```

   حالا سرویس روی پورت 3000 در دسترس است.

> **نکته:** اگر فونت‌های سفارشی یا فایل jar تغییر کند، باید ایمیج جدید بسازید.

### استفاده از ایمیج آماده Docker

شما می‌توانید بدون نیاز به ساخت ایمیج، مستقیماً از ایمیج آماده در Docker Hub استفاده کنید:

```sh
docker pull ahmadmehrabix/html2pdf:latest
docker run -p 3000:3000 ahmadmehrabix/html2pdf:latest
```

این روش سریع‌ترین راه برای راه‌اندازی سرویس است. در صورت نیاز به شخصی‌سازی (مثلاً تغییر فونت یا jar)، می‌توانید طبق مراحل قبلی ایمیج اختصاصی خود را بسازید.

---
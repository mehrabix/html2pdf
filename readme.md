# HTML2PDF API Usage Guide

## ارسال درخواست (Body) برای تولید PDF

برای استفاده از این سرویس، باید یک درخواست POST به آدرس `/api/pdf/sync` ارسال کنید و body را به صورت JSON زیر تنظیم نمایید:

### نمونه کامل درخواست (Body) برای تولید PDF فارسی با امکانات پیشرفته

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
  "scheduledTime": null,
  "rtl": true,
  "headerHtml": "<html><head><meta charset='UTF-8'><style>body{font-family:Tahoma,'Vazir',Arial,'Arial Unicode MS',sans-serif;font-size:10pt;direction:rtl;margin:0;}</style></head><body><div style='text-align:center;'>سربرگ سفارشی</div></body></html>",
  "footerHtml": "<html><head><meta charset='UTF-8'></head><body style='font-family:Tahoma; font-size:10pt; text-align:center; direction:ltr; unicode-bidi:embed;'>صفحه [page] از [toPage]</body></html>",
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

### توضیح فیلدها
- **htmlPages**: آرایه‌ای از رشته‌های HTML. هر رشته یک صفحه است.
- **globalOptions**: تنظیمات کلی wkhtmltopdf (سایز، جهت، حاشیه و ...).
- **pageOptions**: تنظیمات اختصاصی هر صفحه (معمولاً خالی بگذارید اگر از footerHtml استفاده می‌کنید).
- **rtl**: اگر true باشد، متن راست به چپ خواهد بود (برای فارسی).
- **headerHtml**: HTML کامل برای سربرگ (header) PDF.
- **footerHtml**: HTML کامل برای فوتر (footer) PDF. برای نمایش شماره صفحه، از `[page]` و `[toPage]` استفاده کنید. توجه: wkhtmltopdf فقط در حالت ساده و با نسخه رسمی این متغیرها را جایگزین می‌کند.
- **addToc**: اگر true باشد، فهرست مطالب (Table of Contents) اضافه می‌شود.
- **outline**: اگر true باشد، بوکمارک/آوت‌لاین PDF فعال می‌شود.
- **dumpOutline**: نام فایل XML برای خروجی outline (در مسیر temp سیستم ذخیره می‌شود).
- **سایر گزینه‌ها**: grayscale (سیاه و سفید)، lowQuality (کیفیت پایین)، logLevel (سطح لاگ)، copies (تعداد نسخه)، title (عنوان PDF)، printMediaType (استفاده از CSS چاپ).

### نکات مهم درباره فوتر و شماره صفحه
- برای نمایش صحیح شماره صفحه، فقط از `[page]` و `[toPage]` به صورت متن ساده در body فوتر استفاده کنید.
- اگر همچنان `[page]` و `[toPage]` به صورت متن نمایش داده می‌شوند، نسخه wkhtmltopdf خود را بررسی کنید و از نسخه رسمی و به‌روز استفاده نمایید.
- اگر از فونت فارسی خاصی استفاده می‌کنید، آن را در CSS فوتر تعریف کنید.

### نمونه ساده انگلیسی
```json
{
  "htmlPages": [
    "<html><body><h1>Hello World!</h1><p>This is a test page.</p></body></html>"
  ],
  "globalOptions": {
    "--page-size": "A4"
  },
  "rtl": false
}
```

---

برای سوالات بیشتر یا رفع اشکال، لطفاً با پشتیبانی تماس بگیرید یا issue ثبت کنید.

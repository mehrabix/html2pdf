# HTML to PDF Service Usage Guide

This service allows you to convert HTML code to PDF files with extensive options. You can send your request synchronously (sync) for instant file retrieval, or asynchronously (async) for heavy and scheduled jobs.

## Endpoints

- `POST /api/pdf/sync`: For immediate conversion and instant PDF file retrieval.
- `POST /api/pdf/async`: To submit a conversion job, receive a job ID, and check its status later.
- `GET /api/pdf/job/{id}/status`: To check the status of an async job.
- `GET /api/pdf/job/{id}/result`: To download the PDF file of a completed job (the job is deleted after download).

## Full Request Structure (Body)

Send your request as JSON using the following structure. All fields are optional except for `htmlPages`.

```json
{
  "htmlPages": [
    "<html><head></head><body><h1>Hello World!</h1><h2>Section 1</h2><p>This is a sample right-to-left test page.</p><h2>Section 2</h2><p>Continued test...</p></body></html>"
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
  "headerHtml": "<html><head><meta charset='UTF-8'></head><body><div style='text-align:center; font-family:Vazirmatn,tahoma; font-size:10pt;'>Custom Header</div></body></html>",
  "footerHtml": "<html><head><meta charset='UTF-8'></head><body style='font-family:Vazirmatn,tahoma; font-size:10pt; text-align:center;'>Custom Footer</body></html>",
  "addPageNumbering": true,
  "addToc": true,
  "outline": true,
  "dumpOutline": "outline.xml",
  "grayscale": false,
  "lowQuality": false,
  "logLevel": "info",
  "copies": 1,
  "title": "Sample Persian Document",
  "printMediaType": true
}
```

### Field Descriptions

- `htmlPages` **(required)**: An array of strings, each containing the HTML content for a PDF page.
- `globalOptions`: An object with global `wkhtmltopdf` options applied to all pages. For a full list, see the [wkhtmltopdf documentation](https://wkhtmltopdf.org/usage/wkhtmltopdf.txt).
- `pageOptions`: Array of objects for per-page options.
- `scheduledTime`: (async only) Schedule job execution in the future (ISO-8601 format). Example: `"2025-07-08T15:00:00Z"`.
- `rtl`: If `true`, loads the Vazirmatn font for Persian and sets document direction to RTL. Default is `true`.
- `headerHtml`: Full HTML for the header of all pages.
- `footerHtml`: Full HTML for the footer. **Note:** Used only if `addPageNumbering` is `false`.
- `addPageNumbering`: If `true`, adds automatic page numbering ("Page X of Y") to the footer. Recommended for reliable page numbers and takes precedence over `footerHtml`.
- `addToc`: If `true`, adds a table of contents.
- `outline`, `dumpOutline`, `grayscale`, `lowQuality`, `logLevel`, `copies`, `title`, `printMediaType`: Additional options for advanced usage.

## Docker Usage

This project includes a ready-to-use Dockerfile that installs all dependencies for the HTML to PDF service, including:

- Java (eclipse-temurin:23-jre)
- Required libraries and fonts for wkhtmltopdf and Persian/RTL support
- Specific version of wkhtmltopdf and libjpeg-turbo
- Custom Vazirmatn font for better Persian text support
- Copies the built jar file into the image

### Build and Run with Docker

1. **Build the jar file** (if needed):

   Ensure the project jar is built:

   ```sh
   ./mvnw clean package
   ```
   or
   ```sh
   mvn clean package
   ```

   The jar file should be at `target/html2pdf-0.0.1-SNAPSHOT.jar`.

2. **Build the Docker image:**

   In the project root, run:

   ```sh
   docker build -t html2pdf-service .
   ```

3. **Run the service:**

   ```sh
   docker run -p 3000:3000 html2pdf-service
   ```

   The service will be available on port 3000.

> **Note:** If you change custom fonts or the jar file, rebuild the image.

### Use the Prebuilt Docker Image

You can use the prebuilt image from Docker Hub without building locally:
https://hub.docker.com/r/ahmadmehrabix/html2pdf

```sh
docker pull ahmadmehrabix/html2pdf:latest
docker run -p 3000:3000 ahmadmehrabix/html2pdf:latest
```

This is the fastest way to get started. For customization (e.g., changing fonts or the jar), build your own image as described above. 

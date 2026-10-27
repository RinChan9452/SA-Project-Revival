# Open Report.docx in Word, update every field (TOC, list of figures / tables, SEQ, PAGE), save, and export Report.pdf.
$ErrorActionPreference = "Stop"
$report = Join-Path (Split-Path -Parent $PSScriptRoot) "Report.docx"
$pdf = [System.IO.Path]::ChangeExtension($report, ".pdf")
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
try {
    $doc = $word.Documents.Open($report)
    $doc.Fields.Update() | Out-Null
    $doc.Repaginate()
    foreach ($t in $doc.TablesOfContents) { $t.Update() }
    foreach ($t in $doc.TablesOfFigures) { $t.Update() }
    $doc.Fields.Update() | Out-Null
    foreach ($t in $doc.TablesOfContents) { $t.UpdatePageNumbers() }
    foreach ($t in $doc.TablesOfFigures) { $t.UpdatePageNumbers() }
    $doc.Save()
    $doc.ExportAsFixedFormat($pdf, 17)   # wdExportFormatPDF
    "pages: " + $doc.ComputeStatistics(2)
    $doc.Close()
} finally {
    $word.Quit()
}
"saved $report and $pdf"

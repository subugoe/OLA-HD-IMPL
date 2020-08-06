<template>
    <section id="about">
        <div class="container">
            <div class="row mt-3">
                <div class="col">
                    <h2>
                        OLA-HD: <small class="text-muted">Über das Projekt</small>
                    </h2>
                    <hr>
                    <p>
                        OLA-HD ist ein Kooperationsprojekt der Niedersächsischen Staats- und Universitätsbibliothek
                        Göttingen und der <a href="https://www.gwdg.de/" target="_blank">Gesellschaft für wissenschaftliche
                        Datenverarbeitung mbH Göttingen</a>, welches als Modul dem Dachprojekt
                        <a href="https://ocr-d.de/" target="_blank">Koordinierungsprojekt zur Weiterentwicklung von Verfahren der
                        Optical Character Recognition (OCR)</a> – der <a href="https://www.dfg.de/" target="_blank">Deutschen
                        Forschungsgemeinschaft</a> zugeordnet ist.
                    </p>
                    <p>
                        Im regelmäßigen Austausch mit den Projektpartnern wurden die Basisanforderungen für die
                        Langzeitarchivierung und persistente Identifikation ermittelt und in Form einer Spezifikation
                        zur technischen und wirtschaftlich-organisatorischen Umsetzung festgehalten.
                    </p>
                    <p>
                        Mit dem OLA-HD-Prototypen kann der Anwender OCR-Ergebnisse eines Werkes als komprimiertes
                        Dateiformat (OCRD-ZIP) in das System laden. Das System validiert die Datei, vergibt eine PID und
                        schickt die Datei an den Archiv-Manager (<a href="https://cdstar.gwdg.de/" target="_blank">CDSTAR – GWDG Common
                        Data Storage Architecture</a>). Dieser schreibt die Datei in das Archiv (Bandspeicher) und
                        indexiert die Daten für die Suche. Abhängig von der Konfiguration (Datei-Typ, Datei-Größe etc.)
                        werden Dateien zusätzlich in einem Online-Speicher (Festplatte) gespeichert, um einen schnellen
                        Zugriff zu ermöglichen. Der Nutzer hat dabei Zugriff auf alle OCR-Versionen und kann diese zudem
                        als OCRD-ZIP Dateien herunterladen. Alle Werke und Versionen haben eigene PIDs. Die PIDs werden
                        vom <a href="https://www.pidconsortium.net/" target="_blank">European Persistent Identifier Consortium</a>
                        (ePIC) Service generiert. Die verschiedenen OCR-Versionen eines Werkes sind über die PID
                        verknüpft, so dass das Datenmanagementsystem die Versionierung in einer Baumstruktur abbilden kann.
                    </p>

                    <h4>OLA-HD Konzept</h4>
                    <p>
                        Langzeitarchivierung und persistente Identifizierung von OCR-Objekten
                        (<a href="https://github.com/subugoe/OLA-HD-IMPL/blob/master/docs/OLA-HD_Konzept.pdf" target="_blank">Konzept</a>)
                    </p>

                    <h4>Über die OLA-HD Implementierung</h4>
                    <ul>
                        <li>Autor: SUB Göttingen und GWDG</li>
                        <li>Lizenz: Apache License 2.0</li>
                        <li>Version: 1.0.0</li>
                        <li>
                            <a href="https://github.com/subugoe/OLA-HD-IMPL" target="_blank">Dokumentation und Quellcode</a>
                        </li>
                        <li>
                            <a href="https://github.com/subugoe/OLA-HD-IMPL/issues" target="_blank">Fehler melden</a>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </section>
</template>

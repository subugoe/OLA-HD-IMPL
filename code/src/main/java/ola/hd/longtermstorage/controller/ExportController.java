package ola.hd.longtermstorage.controller;

import ola.hd.longtermstorage.domain.ResponseMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExportController {

    @GetMapping(value = "/export")
    public ResponseEntity<?> export(@RequestParam(name = "ppn", required = false) String ppn,
                                    @RequestParam(name = "pid", required = false) String pid) {

        if (ppn == null && pid == null) {
            return new ResponseEntity<>(
                    new ResponseMessage(HttpStatus.BAD_REQUEST, "A PPN or a PID is required."),
                    HttpStatus.BAD_REQUEST);
        }

        return null;
    }
}

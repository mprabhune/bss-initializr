package com.discover.bss_initializr;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/generate")
public class ProjectGeneratorController {

    @PostMapping
    public ResponseEntity<byte[]> generateProject(@RequestBody ProjectRequest projectRequest) throws IOException {
        // Generate the project structure, including `build.gradle` and source files.
        byte[] zipContent = ProjectGenerator.generate(projectRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename("custom-project.zip").build());

        return new ResponseEntity<>(zipContent, headers, HttpStatus.OK);
    }
}

//package com.interviewpartner.controller;
//
//import com.interviewpartner.entity.Resume;
//import com.interviewpartner.service.FileStorageService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/files")
//@RequiredArgsConstructor
//@Slf4j
//public class FileController {
//
//    private final FileStorageService fileStorageService;
//
//    @PostMapping("/upload-resume")
//    public ResponseEntity<?> uploadResume(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestParam("file") MultipartFile file) {
//        log.info("Uploading resume file for user: {}", userDetails.getUsername());
//        Resume resume = fileStorageService.storeResume(userDetails.getUsername(), file);
//        return ResponseEntity.ok(Map.of(
//                "success", true,
//                "message", "Resume uploaded successfully",
//                "fileName", resume.getFileName(),
//                "fileSize", resume.getFileSize()
//        ));
//    }
//}

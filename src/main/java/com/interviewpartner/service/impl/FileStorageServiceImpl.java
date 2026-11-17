//package com.interviewpartner.service.impl;
//
//import com.interviewpartner.entity.Resume;
//import com.interviewpartner.entity.User;
//import com.interviewpartner.exception.BadRequestException;
//import com.interviewpartner.exception.ResourceNotFoundException;
//import com.interviewpartner.repository.ResumeRepository;
//import com.interviewpartner.repository.UserRepository;
//import com.interviewpartner.service.FileStorageService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.text.PDFTextStripper;
//import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
//import org.apache.poi.xwpf.usermodel.XWPFDocument;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.util.Objects;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class FileStorageServiceImpl implements FileStorageService {
//
//    private final UserRepository userRepository;
//    private final ResumeRepository resumeRepository;
//    private final Path fileStorageLocation = Paths.get("uploads/resumes").toAbsolutePath().normalize();
//
//    @Override
//    @Transactional
//    public Resume storeResume(String email, MultipartFile file) {
//        if (file.isEmpty()) {
//            throw new BadRequestException("Failed to store empty file.");
//        }
//
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
//
//        try {
//            Files.createDirectories(this.fileStorageLocation);
//
//            String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
//            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
//
//            if (!fileExtension.equals(".pdf") && !fileExtension.equals(".docx") && !fileExtension.equals(".doc")) {
//                throw new BadRequestException("Only PDF and DOCX files are allowed.");
//            }
//
//            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
//            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
//            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
//
//            log.info("Saved resume to {}", targetLocation);
//
//            // Extract content from file
//            String parsedText = "";
//            try {
//                parsedText = extractText(targetLocation, fileExtension);
//                log.info("Extracted {} characters from resume {}", parsedText.length(), originalFileName);
//            } catch (Exception e) {
//                log.error("Failed to extract text from file: {}", originalFileName, e);
//                parsedText = "Error extracting text: " + e.getMessage();
//            }
//
//            Resume resume = user.getResume();
//            if (resume == null) {
//                resume = new Resume();
//                resume.setUser(user);
//            } else {
//                try {
//                    Files.deleteIfExists(Paths.get(resume.getFilePath()));
//                } catch (IOException e) {
//                    log.warn("Failed to delete previous resume file: {}", resume.getFilePath(), e);
//                }
//            }
//
//            resume.setFileName(originalFileName);
//            resume.setFileType(file.getContentType());
//            resume.setFilePath(targetLocation.toString());
//            resume.setFileSize(file.getSize());
//            resume.setParsedContent(parsedText);
//
//            Resume savedResume = resumeRepository.save(resume);
//            user.setResume(savedResume);
//            userRepository.save(user);
//
//            return savedResume;
//        } catch (IOException ex) {
//            throw new BadRequestException("Could not store file " + file.getOriginalFilename() + ". Please try again: " + ex.getMessage());
//        }
//    }
//
//    private String extractText(Path filePath, String extension) throws IOException {
//        if (".pdf".equals(extension)) {
//            try (PDDocument document = PDDocument.load(filePath.toFile())) {
//                PDFTextStripper stripper = new PDFTextStripper();
//                return stripper.getText(document);
//            }
//        } else if (".docx".equals(extension)) {
//            try (InputStream fis = Files.newInputStream(filePath);
//                 XWPFDocument document = new XWPFDocument(fis);
//                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
//                return extractor.getText();
//            }
//        } else if (".doc".equals(extension)) {
//            return "Word .doc parsing not fully supported; please upload a .docx or .pdf file instead.";
//        }
//        return "";
//    }
//}

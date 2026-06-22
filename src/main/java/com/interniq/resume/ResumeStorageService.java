package com.interniq.resume;

import com.interniq.candidate.Candidate;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt");

    private final ResumeFileRepository resumeFileRepository;
    private final Tika tika = new Tika();

    @Value("${application.resume.upload-dir}")
    private String uploadDir;

    @Transactional
    public StoredResume store(Candidate candidate, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required");
        }

        String originalFileName = cleanFileName(file.getOriginalFilename());
        String extension = getExtension(originalFileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only PDF, DOC, DOCX, and TXT resume files are supported");
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String storedFileName = "candidate-" + candidate.getId() + "-" + UUID.randomUUID() + "." + extension;
            Path destination = uploadPath.resolve(storedFileName).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            String extractedText = extractText(destination);

            ResumeFile resumeFile = ResumeFile.builder()
                    .candidate(candidate)
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .storagePath(destination.toString())
                    .build();

            return new StoredResume(resumeFileRepository.save(resumeFile), extractedText);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to store resume file");
        }
    }

    private String extractText(Path filePath) {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            String text = tika.parseToString(inputStream);
            return text.length() <= 100_000 ? text : text.substring(0, 100_000);
        } catch (IOException | TikaException ex) {
            throw new IllegalArgumentException("Unable to extract text from resume file");
        }
    }

    private String cleanFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "resume.txt";
        }

        return Paths.get(fileName).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredResume(ResumeFile resumeFile, String extractedText) {
    }
}

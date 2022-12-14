package dev.kkukkie_bookstore.service.member;

import dev.kkukkie_bookstore.controller.member.form.MemberUpdateForm;
import dev.kkukkie_bookstore.model.file.image.ImageExtensionException;
import dev.kkukkie_bookstore.model.file.image.ImageFile;
import dev.kkukkie_bookstore.model.file.image.ImageSizeException;
import dev.kkukkie_bookstore.model.item.book.Book;
import dev.kkukkie_bookstore.model.member.Member;
import dev.kkukkie_bookstore.model.team.Team;
import dev.kkukkie_bookstore.repository.item.BookRepository;
import dev.kkukkie_bookstore.repository.member.MemberRepository;
import dev.kkukkie_bookstore.security.PasswordService;
import dev.kkukkie_bookstore.util.FileManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.management.RuntimeErrorException;
import javax.transaction.Transactional;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class MemberService {

    private final List<String> allowedExtensions = new ArrayList<>();

    private final String profileImgBasePath;
    private final int profileMaxFileSize;

    public static final String PROFILE_IMG_URI = "images";

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    private final PasswordService passwordService;

    public MemberService(Environment environment,
                         BookRepository bookRepository, MemberRepository memberRepository,
                         PasswordService passwordService) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;

        this.profileImgBasePath = environment.getProperty("spring.servlet.multipart.location");
        this.profileMaxFileSize = FileManager.getSizeFromUnit(environment.getProperty("spring.servlet.multipart.maxFileSize"));

        this.passwordService = passwordService;

        allowedExtensions.add("jpeg");
        allowedExtensions.add("jpg");
    }

    @Transactional
    public boolean addBookToList(long memberId, String bookId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member != null) {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null) { return false; }

            Book foundBook = findBookByIdFromMember(member, bookId);
            if (foundBook != null) { return false; }

            Integer count = book.getCount();
            if (count > 0) {
                book.setCount(count - 1);
                bookRepository.save(book);

                member.getBooks().add(book);
                memberRepository.save(member);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Transactional
    public void removeBookFromList(long memberId, String bookId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member != null) {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null) { return; }

            Integer count = book.getCount();
            book.setCount(count + 1);
            bookRepository.save(book);

            member.getBooks().remove(book);
            memberRepository.save(member);
        }
    }

    public Book findBookByIdFromMember(Member member, String bookId) {
        return member.getBooks().stream().filter(
                item1 -> item1.getId().equals(bookId)
        ).findAny().orElse(null);
    }

    public void saveProfileImage(Member member, MultipartFile profileImgFile, String prevProfileImgId)
            throws RuntimeErrorException, IOException {
        // ?????? ?????? ?????? ?????? ??????
        if (profileImgFile.getSize() >= profileMaxFileSize) {
            throw new ImageSizeException();
        }

        String fileName = profileImgFile.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) { return; }

        String extension = FilenameUtils.getExtension(fileName);
        if (extension.isEmpty() || !allowedExtensions.contains(extension)) {
            throw new ImageExtensionException();
        }

        // ?????? ?????? ????????? ???????????? ?????? UUID ??? ???????????? ??????
        String newImgName = prevProfileImgId;
        if (newImgName == null || newImgName.isEmpty()) {
            newImgName = UUID.randomUUID() + "." + extension;
        }

        // ??????
        File file = new File(profileImgBasePath, newImgName);
        profileImgFile.transferTo(file);

        // Resize
        resizeImage(file, extension);

        member.setProfileImgFile(
                new ImageFile(
                        newImgName, // UUID ??? id ??? ???????????? ??????
                        fileName, // ?????? ?????? ??????
                        PROFILE_IMG_URI, // ?????????????????? ????????? REST URI
                        profileImgBasePath, // ?????? ?????? ?????? ?????? (?????? ?????? ??????)
                        profileImgFile.getSize() // ?????? ?????? ??????
                )
        );
    }

    private void resizeImage(File file, String extension) throws IOException {
        Image originalImage = ImageIO.read(file);
        int originWidth = originalImage.getWidth(null);
        int originHeight = originalImage.getHeight(null);
        int newWidth = 300;
        if (originWidth > newWidth) {
            // Reduce width & height
            int newHeight = (originHeight * newWidth) / originWidth;
            BufferedImage newImage = new BufferedImage(
                    newWidth, newHeight,
                    BufferedImage.TYPE_INT_RGB
            );

            // Redraw
            Image resizeImage = originalImage
                    .getScaledInstance(
                            newWidth, newHeight,
                            Image.SCALE_SMOOTH
                    );
            Graphics graphics = newImage.getGraphics();
            graphics.drawImage(resizeImage, 0, 0, null);
            graphics.dispose();

            // Save
            ImageIO.write(newImage, extension, file);
        }
    }

    public Member saveMember(String loginId, String password,
                              String username, String age,
                              Team team,
                              MultipartFile profileImgFile,
                              BindingResult bindingResult) {
        Member member = null;
        try {
            member = new Member(
                    loginId,
                    username,
                    Integer.parseInt(age),
                    team
            );
            member.setPassword(passwordService.encryptPassword(password, member));

            // ???????????? ????????? ????????? Option
            if (profileImgFile != null) {
                saveProfileImage(member, profileImgFile, null);
            }
        } catch (Exception e) {
            if (e.getClass().equals(ImageSizeException.class)) {
                bindingResult.reject("ImageSizeException", new Object[]{profileImgFile.getSize()}, "????????? ????????? ????????? ?????? ?????????.");
            } else if (e.getClass().equals(ImageExtensionException.class)) {
                bindingResult.reject("ImageExtensionException", new Object[]{allowedExtensions.toArray()}, "????????? ????????? ????????? ????????? ???????????? ???????????? ????????????.");
            } else {
                bindingResult.reject("InputException", new Object[]{}, "????????? ????????? ???????????????.");
            }
        }
        return member;
    }

    public void updateMember(MemberUpdateForm memberUpdateForm, BindingResult bindingResult, Member member) {
        if (member != null) {
            try {
                member.setUsername(memberUpdateForm.getUsername());
                member.setAge(Integer.parseInt(memberUpdateForm.getAge()));
                member.setPassword(passwordService.encryptPassword(memberUpdateForm.getPassword(), member));
                member.setRole(memberUpdateForm.getRole());
                member.setTeam(memberUpdateForm.getTeam());

                // ???????????? ????????? ????????? Option
                MultipartFile profileImgFile = memberUpdateForm.getProfileImgFile();
                if ((profileImgFile != null)
                        && (profileImgFile.getOriginalFilename() != null && !profileImgFile.getOriginalFilename().isEmpty())) {
                    // ????????? ???????????? ???????????? ????????? ?????? ?????? ??????
                    String prevProfileImgId = deletePrevProfileImage(member);

                    // ????????? ???????????? ????????? ??????
                    saveProfileImage(member, profileImgFile, prevProfileImgId);
                }
            } catch (Exception e) {
                if (e.getClass().equals(ImageSizeException.class)) {
                    bindingResult.reject("ImageSizeException", new Object[]{memberUpdateForm.getProfileImgFile().getSize()}, "????????? ????????? ????????? ?????? ?????????.");
                } else if (e.getClass().equals(ImageExtensionException.class)) {
                    bindingResult.reject("ImageExtensionException", new Object[]{allowedExtensions.toArray()}, "????????? ????????? ????????? ????????? ???????????? ???????????? ????????????.");
                } else {
                    bindingResult.reject("InputException", new Object[]{}, "????????? ????????? ???????????????.");
                }
            }
        }
    }

    public String deletePrevProfileImage(Member member) {
        ImageFile prevProfileImgFile = member.getProfileImgFile();
        if (prevProfileImgFile != null) {
            // ????????? ?????? ????????? UUID (????????? ??? ???????????? ????????? UUID ??? ?????? ??????)
            FileManager.deleteFile(
                    new File(FileManager.concatFilePath(
                            prevProfileImgFile.getLocalBasePath(), prevProfileImgFile.getId()
                    ))
            );
            return prevProfileImgFile.getId();
        }
        return null;
    }

}

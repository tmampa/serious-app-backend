package com.seriousapp.serious.app.borrowing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/borrow")
@Slf4j
@Deprecated // This controller is deprecated as functionality has been moved to AdminController
public class BorrowRecordController {
    // All functionality has been moved to AdminService and AdminController
}

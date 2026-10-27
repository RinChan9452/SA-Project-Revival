-- ProjectSA : Database schema (SQL Server / T-SQL)
-- Mapped from "ER Diagram (Revised)" (Chen). EMPLOYEE subclasses (T,D) -> one table with Role.

CREATE TABLE EMPLOYEE (
    EmployeeID   INT IDENTITY(1,1) PRIMARY KEY,
    FirstName    NVARCHAR(50)  NOT NULL,
    LastName     NVARCHAR(50)  NOT NULL,
    Email        NVARCHAR(100) NOT NULL UNIQUE,
    Phone        NVARCHAR(20)  NULL,
    Username     NVARCHAR(50)  NOT NULL UNIQUE,
    Password     NVARCHAR(255) NOT NULL,
    ProfilePicture NVARCHAR(255) NULL,           -- path ใน ./uploads/profile/ (Profile P.5)
    Role         NVARCHAR(20)  NOT NULL CHECK (Role IN ('Sale','Presale','PM','TechEngineer'))
);

CREATE TABLE SKILL (
    SkillID      INT IDENTITY(1,1) PRIMARY KEY,
    SkillName    NVARCHAR(50)  NOT NULL UNIQUE,
    Description  NVARCHAR(255) NULL
);

CREATE TABLE ENGINEER_SKILL (
    EmployeeID   INT NOT NULL REFERENCES EMPLOYEE(EmployeeID),
    SkillID      INT NOT NULL REFERENCES SKILL(SkillID),
    PRIMARY KEY (EmployeeID, SkillID)
);

CREATE TABLE CUSTOMER (
    CustomerID     INT IDENTITY(1,1) PRIMARY KEY,
    CustomerName   NVARCHAR(100) NOT NULL,
    CustomerEmail  NVARCHAR(100) NOT NULL
);

CREATE TABLE PROJECT (
    ProjectID        INT IDENTITY(1,1) PRIMARY KEY,
    ProjectName      NVARCHAR(100) NOT NULL,
    ProjectSite      NVARCHAR(255) NOT NULL,
    Status           NVARCHAR(30)  NOT NULL DEFAULT 'NewProject'
                     CHECK (Status IN ('NewProject','WaitingForAssignEngineer','Working',
                                       'Testing','WaitingForApprove','Finish')),
    CreatedDate      DATETIME      NOT NULL DEFAULT GETDATE(),
    StartDate        DATE          NULL,          -- Period
    EndDate          DATE          NULL,          -- Period
    Objective        NVARCHAR(MAX) NULL,          -- Solution
    EstimatedBudget  DECIMAL(12,2) NULL,          -- Solution
    SolutionDetail   NVARCHAR(MAX) NULL,          -- Solution
    CustomerID       INT NOT NULL REFERENCES CUSTOMER(CustomerID),   -- Owns
    SaleID           INT NOT NULL REFERENCES EMPLOYEE(EmployeeID),   -- Creates
    PresaleID        INT NOT NULL REFERENCES EMPLOYEE(EmployeeID),   -- Designs
    PMID             INT NULL     REFERENCES EMPLOYEE(EmployeeID)    -- Manages
);

CREATE TABLE PROJECT_ASSIGNMENT (                 -- AssignedTo (M:N)
    ProjectID     INT NOT NULL REFERENCES PROJECT(ProjectID),
    EngineerID    INT NOT NULL REFERENCES EMPLOYEE(EmployeeID),
    AssignedDate  DATETIME NOT NULL DEFAULT GETDATE(),
    PRIMARY KEY (ProjectID, EngineerID)
);

CREATE TABLE TEST_SCHEDULE (
    ScheduleID    INT IDENTITY(1,1) PRIMARY KEY,
    ProjectID     INT NOT NULL REFERENCES PROJECT(ProjectID),        -- Has
    CreatedByID   INT NOT NULL REFERENCES EMPLOYEE(EmployeeID),      -- Sets
    TestDate      DATE          NOT NULL,
    StartTime     TIME          NOT NULL,
    EndTime       TIME          NOT NULL,
    Location      NVARCHAR(255) NOT NULL,
    TestDetail    NVARCHAR(MAX) NULL,
    RemindBefore  INT           NOT NULL,          -- minutes
    Status        NVARCHAR(20)  NOT NULL DEFAULT 'Scheduled'
                  CHECK (Status IN ('Scheduled','Cancelled','Done')),
    CreatedDate   DATETIME      NOT NULL DEFAULT GETDATE()
);

CREATE TABLE CUSTOMER_REQUIREMENT (
    RequirementID   INT IDENTITY(1,1) PRIMARY KEY,
    ProjectID       INT NOT NULL REFERENCES PROJECT(ProjectID),      -- Contains
    Title           NVARCHAR(150) NOT NULL,
    Detail          NVARCHAR(MAX) NOT NULL,
    Priority        NVARCHAR(10)  NOT NULL CHECK (Priority IN ('Low','Medium','High')),
    DueDate         DATE          NOT NULL,
    RelatedFeature  NVARCHAR(150) NULL,
    Status          NVARCHAR(20)  NOT NULL DEFAULT 'Pending'
                    CHECK (Status IN ('Pending','Approved','Rejected')),
    CreatedByID     INT NOT NULL REFERENCES EMPLOYEE(EmployeeID),    -- Records (PM)
    CreatedDate     DATETIME      NOT NULL DEFAULT GETDATE(),
    ReviewedByID    INT NULL     REFERENCES EMPLOYEE(EmployeeID),    -- Approves (Sale)
    ReviewedDate    DATETIME      NULL,
    RejectReason    NVARCHAR(255) NULL
);

CREATE TABLE REQUIREMENT_ATTACHMENT (             -- multivalued AttachmentFile
    RequirementID   INT NOT NULL REFERENCES CUSTOMER_REQUIREMENT(RequirementID),
    AttachmentFile  NVARCHAR(255) NOT NULL,
    PRIMARY KEY (RequirementID, AttachmentFile)
);

CREATE TABLE PROJECT_DOCUMENT (
    DocumentID    INT IDENTITY(1,1) PRIMARY KEY,
    ProjectID     INT NOT NULL REFERENCES PROJECT(ProjectID),        -- Stores
    DocType       NVARCHAR(30)  NOT NULL CHECK (DocType IN ('Solution','ProductRequirement')),
    FileName      NVARCHAR(255) NOT NULL,
    FilePath      NVARCHAR(255) NOT NULL,
    FileSize      INT           NOT NULL,          -- bytes
    UploadedDate  DATETIME      NOT NULL DEFAULT GETDATE()
);

CREATE TABLE NOTIFICATION (
    NotificationID  INT IDENTITY(1,1) PRIMARY KEY,
    RecipientID     INT NOT NULL REFERENCES EMPLOYEE(EmployeeID),    -- Receives
    ProjectID       INT NOT NULL REFERENCES PROJECT(ProjectID),      -- Triggers
    Type            NVARCHAR(30)  NOT NULL,
    Message         NVARCHAR(255) NOT NULL,
    SendAt          DATETIME      NOT NULL,
    IsRead          BIT           NOT NULL DEFAULT 0
);


-- ============================================================
-- UC01 Create Project Profile  (Actor: Sale)
-- ============================================================
-- Query 2.1: Sale in Charge = ผู้ใช้ที่ login
SELECT EmployeeID, FirstName, LastName
FROM EMPLOYEE
WHERE EmployeeID = @LoginID AND Role = 'Sale';

-- Query 2.2: รายชื่อลูกค้าเดิม
SELECT CustomerID, CustomerName, CustomerEmail
FROM CUSTOMER
ORDER BY CustomerName;

-- Query 2.3: รายชื่อ Presale Engineer
SELECT EmployeeID, FirstName, LastName
FROM EMPLOYEE
WHERE Role = 'Presale'
ORDER BY FirstName;

-- Query 5.1: กรณีลูกค้าใหม่
INSERT INTO CUSTOMER (CustomerName, CustomerEmail)
VALUES (@CustomerName, @CustomerEmail);
SET @CustomerID = SCOPE_IDENTITY();

-- Query 5.2: ProjectID สร้างอัตโนมัติจาก IDENTITY(1,1)
INSERT INTO PROJECT (ProjectName, ProjectSite, Status, CreatedDate, CustomerID, SaleID, PresaleID)
VALUES (@ProjectName, @ProjectSite, 'NewProject', GETDATE(), @CustomerID, @LoginID, @PresaleID);
SET @ProjectID = SCOPE_IDENTITY();

-- Query 6.1: Status = 'NewProject' ถูกบันทึกพร้อม Query 5.2
-- (no statement)

-- Query 7.1
INSERT INTO NOTIFICATION (RecipientID, ProjectID, Type, Message, SendAt, IsRead)
VALUES (@PresaleID, @ProjectID, 'NewProject',
        CONCAT(N'มี Project ใหม่รอออกแบบ Solution: ', @ProjectName), GETDATE(), 0);

-- ============================================================
-- UC02 Store Project Solution Details  (Actor: Presale Engineer)
-- ============================================================
-- Query 1.1
SELECT p.ProjectID, p.ProjectName, c.CustomerName
FROM PROJECT p
JOIN CUSTOMER c ON c.CustomerID = p.CustomerID
WHERE p.PresaleID = @LoginID AND p.Status = 'NewProject'
ORDER BY p.CreatedDate DESC;

-- Query 3.1: ข้อมูล Project + Solution เดิม (ถ้ามี)
SELECT p.ProjectID, p.ProjectName, p.ProjectSite, c.CustomerName,
       p.StartDate, p.EndDate, p.Objective, p.EstimatedBudget, p.SolutionDetail
FROM PROJECT p
JOIN CUSTOMER c ON c.CustomerID = p.CustomerID
WHERE p.ProjectID = @ProjectID AND p.PresaleID = @LoginID;

-- Query 3.2: เอกสารที่เคยอัปโหลด
SELECT DocumentID, DocType, FileName, UploadedDate
FROM PROJECT_DOCUMENT
WHERE ProjectID = @ProjectID
ORDER BY UploadedDate DESC;

-- Query 8.1
UPDATE PROJECT
SET StartDate = @StartDate, EndDate = @EndDate, Objective = @Objective,
    EstimatedBudget = @EstimatedBudget, SolutionDetail = @SolutionDetail
WHERE ProjectID = @ProjectID;

-- Query 8.2: ทำซ้ำทุกไฟล์, @DocType = 'Solution' / 'ProductRequirement'
INSERT INTO PROJECT_DOCUMENT (ProjectID, DocType, FileName, FilePath, FileSize, UploadedDate)
VALUES (@ProjectID, @DocType, @FileName, @FilePath, @FileSize, GETDATE());

-- Query 9.1
UPDATE PROJECT
SET Status = 'WaitingForAssignEngineer'
WHERE ProjectID = @ProjectID;

-- Query 10.1: แจ้ง PM ทุกคน
INSERT INTO NOTIFICATION (RecipientID, ProjectID, Type, Message, SendAt, IsRead)
SELECT EmployeeID, @ProjectID, 'WaitingAssign',
       N'มี Project รอมอบหมาย Engineer', GETDATE(), 0
FROM EMPLOYEE
WHERE Role = 'PM';

-- ============================================================
-- UC03 Check Project Details  (Actor: Sale, Presale Engineer, PM, Tech Engineer)
-- ============================================================
-- Query 1.1: @Role / @LoginID จากผู้ใช้ที่ login, @Keyword ใช้ค้นหาใน Step 2 (NULL = แสดงทั้งหมด)
SELECT p.ProjectID, p.ProjectName, c.CustomerName, p.Status
FROM PROJECT p
JOIN CUSTOMER c ON c.CustomerID = p.CustomerID
WHERE (@Role = 'PM' OR p.SaleID = @LoginID OR p.PresaleID = @LoginID
       OR EXISTS (SELECT 1 FROM PROJECT_ASSIGNMENT pa
                  WHERE pa.ProjectID = p.ProjectID AND pa.EngineerID = @LoginID))
  AND (@Keyword IS NULL OR p.ProjectName LIKE CONCAT('%', @Keyword, '%')
       OR CAST(p.ProjectID AS NVARCHAR(10)) = @Keyword)
ORDER BY p.ProjectID DESC;

-- Query 3.1: ผลลัพธ์ = 0 → Alternative 3a
SELECT COUNT(*) AS Found
FROM PROJECT p
WHERE p.ProjectID = @ProjectID
  AND (@Role = 'PM' OR p.SaleID = @LoginID OR p.PresaleID = @LoginID
       OR EXISTS (SELECT 1 FROM PROJECT_ASSIGNMENT pa
                  WHERE pa.ProjectID = p.ProjectID AND pa.EngineerID = @LoginID));

-- Query 4.1: Step 4.1 – 4.4
SELECT p.ProjectID, p.ProjectName, p.Status, p.ProjectSite,
       c.CustomerName, c.CustomerEmail,
       CONCAT(s.FirstName, ' ', s.LastName)   AS SaleInCharge,
       CONCAT(ps.FirstName, ' ', ps.LastName) AS PresaleInCharge,
       CASE WHEN p.PMID IS NULL THEN NULL
            ELSE CONCAT(pm.FirstName, ' ', pm.LastName) END AS PMName,
       p.StartDate, p.EndDate, p.Objective, p.EstimatedBudget, p.SolutionDetail
FROM PROJECT p
JOIN CUSTOMER c       ON c.CustomerID  = p.CustomerID
JOIN EMPLOYEE s       ON s.EmployeeID  = p.SaleID
JOIN EMPLOYEE ps      ON ps.EmployeeID = p.PresaleID
LEFT JOIN EMPLOYEE pm ON pm.EmployeeID = p.PMID
WHERE p.ProjectID = @ProjectID;

-- Query 4.2: Step 4.3 Tech Engineer ที่ถูกมอบหมาย
SELECT e.EmployeeID, e.FirstName, e.LastName, pa.AssignedDate
FROM PROJECT_ASSIGNMENT pa
JOIN EMPLOYEE e ON e.EmployeeID = pa.EngineerID
WHERE pa.ProjectID = @ProjectID;

-- Query 4.3: Step 4.5 นัดหมายทดสอบล่าสุด
SELECT TOP 1 TestDate, StartTime, EndTime, Location, TestDetail
FROM TEST_SCHEDULE
WHERE ProjectID = @ProjectID AND Status IN ('Scheduled', 'Done')
ORDER BY TestDate DESC;

-- Query 4.4: Step 4.6 New Requirement
SELECT RequirementID, Title, Priority, DueDate, Status
FROM CUSTOMER_REQUIREMENT
WHERE ProjectID = @ProjectID
ORDER BY CreatedDate DESC;

-- Query 4.5: Step 4.7 เอกสาร Solution / Product Requirement
SELECT DocumentID, DocType, FileName, UploadedDate
FROM PROJECT_DOCUMENT
WHERE ProjectID = @ProjectID
ORDER BY UploadedDate DESC;

-- Query 4.6: Step 4.7 เอกสารแนบของ New Requirement
SELECT ra.RequirementID, r.Title, ra.AttachmentFile
FROM REQUIREMENT_ATTACHMENT ra
JOIN CUSTOMER_REQUIREMENT r ON r.RequirementID = ra.RequirementID
WHERE r.ProjectID = @ProjectID;

-- Query 6.1: เอกสาร Project
SELECT FileName, FilePath
FROM PROJECT_DOCUMENT
WHERE DocumentID = @DocumentID AND ProjectID = @ProjectID;

-- Query 6.2: เอกสารแนบ Requirement
SELECT ra.AttachmentFile
FROM REQUIREMENT_ATTACHMENT ra
JOIN CUSTOMER_REQUIREMENT r ON r.RequirementID = ra.RequirementID
WHERE ra.RequirementID = @RequirementID AND r.ProjectID = @ProjectID
  AND ra.AttachmentFile = @AttachmentFile;

-- ============================================================
-- UC04 Assign Engineer to Project  (Actor: PM)
-- ============================================================
-- Query 1.1
SELECT p.ProjectID, p.ProjectName, c.CustomerName
FROM PROJECT p
JOIN CUSTOMER c ON c.CustomerID = p.CustomerID
WHERE p.Status = 'WaitingForAssignEngineer'
ORDER BY p.CreatedDate;

-- Query 3.1
SELECT ProjectID, ProjectName, StartDate, EndDate, Objective
FROM PROJECT
WHERE ProjectID = @ProjectID;

-- Query 3.2: Engineer + Skill + จำนวน Project ที่ทำอยู่ + อยู่ใน Project นี้แล้วหรือไม่
SELECT e.EmployeeID, e.FirstName, e.LastName,
       STRING_AGG(s.SkillName, ', ') AS Skills,
       (SELECT COUNT(*) FROM PROJECT_ASSIGNMENT x
        JOIN PROJECT y ON y.ProjectID = x.ProjectID
        WHERE x.EngineerID = e.EmployeeID AND y.Status IN ('Working','Testing')) AS ActiveProjects,
       CASE WHEN EXISTS (SELECT 1 FROM PROJECT_ASSIGNMENT z
                         WHERE z.ProjectID = @ProjectID AND z.EngineerID = e.EmployeeID)
            THEN 1 ELSE 0 END AS InThisProject
FROM EMPLOYEE e
LEFT JOIN ENGINEER_SKILL es ON es.EmployeeID = e.EmployeeID
LEFT JOIN SKILL s           ON s.SkillID     = es.SkillID
WHERE e.Role = 'TechEngineer'
GROUP BY e.EmployeeID, e.FirstName, e.LastName;

-- Query 3.3: ตัวเลือก Skill สำหรับ Filter
SELECT SkillID, SkillName
FROM SKILL
ORDER BY SkillName;

-- Query 5.1
SELECT e.EmployeeID, e.FirstName, e.LastName,
       STRING_AGG(s.SkillName, ', ') AS Skills,
       (SELECT COUNT(*) FROM PROJECT_ASSIGNMENT x
        JOIN PROJECT y ON y.ProjectID = x.ProjectID
        WHERE x.EngineerID = e.EmployeeID AND y.Status IN ('Working','Testing')) AS ActiveProjects
FROM EMPLOYEE e
JOIN ENGINEER_SKILL es ON es.EmployeeID = e.EmployeeID
JOIN SKILL s           ON s.SkillID     = es.SkillID
WHERE e.Role = 'TechEngineer'
  AND EXISTS (SELECT 1 FROM ENGINEER_SKILL f
              WHERE f.EmployeeID = e.EmployeeID AND f.SkillID = @SkillID)
GROUP BY e.EmployeeID, e.FirstName, e.LastName;

-- Query 8.1: ทำซ้ำทุก Engineer ที่เลือก (ไม่เพิ่มซ้ำคนที่อยู่แล้ว)
INSERT INTO PROJECT_ASSIGNMENT (ProjectID, EngineerID, AssignedDate)
SELECT @ProjectID, @EngineerID, GETDATE()
WHERE NOT EXISTS (SELECT 1 FROM PROJECT_ASSIGNMENT
                  WHERE ProjectID = @ProjectID AND EngineerID = @EngineerID);

-- Query 8.2: PM ผู้มอบหมาย = PM ของ Project
UPDATE PROJECT
SET PMID = @LoginID
WHERE ProjectID = @ProjectID;

-- Query 9.1
UPDATE PROJECT
SET Status = 'Working'
WHERE ProjectID = @ProjectID;

-- Query 10.1
INSERT INTO NOTIFICATION (RecipientID, ProjectID, Type, Message, SendAt, IsRead)
SELECT pa.EngineerID, @ProjectID, 'Assigned',
       CONCAT(N'คุณถูกมอบหมายใน Project: ', p.ProjectName), GETDATE(), 0
FROM PROJECT_ASSIGNMENT pa
JOIN PROJECT p ON p.ProjectID = pa.ProjectID
WHERE pa.ProjectID = @ProjectID;

-- ============================================================
-- UC05 Set Project Test Calendar Notification  (Actor: Tech Engineer)
-- ============================================================
-- Query 1.1
SELECT p.ProjectID, p.ProjectName, c.CustomerName, p.Status
FROM PROJECT p
JOIN CUSTOMER c           ON c.CustomerID = p.CustomerID
JOIN PROJECT_ASSIGNMENT pa ON pa.ProjectID = p.ProjectID
WHERE pa.EngineerID = @LoginID AND p.Status IN ('Working','Testing');

-- Query 3.1
SELECT p.ProjectSite, c.CustomerName, c.CustomerEmail
FROM PROJECT p
JOIN CUSTOMER c ON c.CustomerID = p.CustomerID
WHERE p.ProjectID = @ProjectID;

-- Query 3.2: Engineer ในทีม
SELECT e.FirstName, e.LastName
FROM PROJECT_ASSIGNMENT pa
JOIN EMPLOYEE e ON e.EmployeeID = pa.EngineerID
WHERE pa.ProjectID = @ProjectID;

-- Query 3.3: นัดหมายเดิม
SELECT ScheduleID, TestDate, StartTime, EndTime, Location
FROM TEST_SCHEDULE
WHERE ProjectID = @ProjectID AND Status = 'Scheduled';

-- Query 6.1: ยกเลิกนัดเดิม (ถ้ามี)
UPDATE TEST_SCHEDULE
SET Status = 'Cancelled'
WHERE ProjectID = @ProjectID AND Status = 'Scheduled';

-- Query 6.2
INSERT INTO TEST_SCHEDULE (ProjectID, CreatedByID, TestDate, StartTime, EndTime,
                           Location, TestDetail, RemindBefore, Status, CreatedDate)
VALUES (@ProjectID, @LoginID, @TestDate, @StartTime, @EndTime,
        @Location, @TestDetail, @RemindBefore, 'Scheduled', GETDATE());
SET @ScheduleID = SCOPE_IDENTITY();

-- Query 7.1
UPDATE PROJECT
SET Status = 'Testing'
WHERE ProjectID = @ProjectID;

-- Query 8.1: แจ้ง PM + Engineer ทุกคนใน Project (อีเมลลูกค้าใช้ CustomerEmail จาก Query 3.1)
INSERT INTO NOTIFICATION (RecipientID, ProjectID, Type, Message, SendAt, IsRead)
SELECT r.EmployeeID, @ProjectID, 'TestSchedule',
       CONCAT(N'นัดทดสอบ ', @TestDate, ' ', LEFT(CONCAT(@StartTime, ''), 5), N' ที่ ', @Location), GETDATE(), 0
FROM (SELECT EngineerID AS EmployeeID FROM PROJECT_ASSIGNMENT WHERE ProjectID = @ProjectID
      UNION
      SELECT PMID FROM PROJECT WHERE ProjectID = @ProjectID AND PMID IS NOT NULL) r;

-- Query 9.1: แจ้งเตือนล่วงหน้าตาม RemindBefore (นาที)
INSERT INTO NOTIFICATION (RecipientID, ProjectID, Type, Message, SendAt, IsRead)
SELECT r.EmployeeID, @ProjectID, 'TestReminder',
       CONCAT(N'เตือน: นัดทดสอบ ', @TestDate, ' ', LEFT(CONCAT(@StartTime, ''), 5), N' ที่ ', @Location),
       DATEADD(MINUTE, -@RemindBefore, CAST(CONCAT(@TestDate, ' ', LEFT(CONCAT(@StartTime, ''), 8)) AS DATETIME)), 0
FROM (SELECT EngineerID AS EmployeeID FROM PROJECT_ASSIGNMENT WHERE ProjectID = @ProjectID
      UNION
      SELECT PMID FROM PROJECT WHERE ProjectID = @ProjectID AND PMID IS NOT NULL) r;

-- ============================================================
-- UC06 Add Customer New Requirement  (Actor: PM)
-- ============================================================
-- Query 1.1
SELECT p.ProjectID, p.ProjectName, c.CustomerName, p.Status
FROM PROJECT p
JOIN CUSTOMER c ON c.CustomerID = p.CustomerID
WHERE p.PMID = @LoginID AND p.Status IN ('Testing','WaitingForApprove');

-- Query 3.1: Step 3.1 – 3.3, 3.5 – 3.7
SELECT p.ProjectName, c.CustomerName,
       CONCAT(pm.FirstName, ' ', pm.LastName) AS PMName,
       p.Status, p.StartDate, p.SolutionDetail
FROM PROJECT p
JOIN CUSTOMER c  ON c.CustomerID  = p.CustomerID
JOIN EMPLOYEE pm ON pm.EmployeeID = p.PMID
WHERE p.ProjectID = @ProjectID;

-- Query 3.2: Step 3.4
SELECT e.FirstName, e.LastName
FROM PROJECT_ASSIGNMENT pa
JOIN EMPLOYEE e ON e.EmployeeID = pa.EngineerID
WHERE pa.ProjectID = @ProjectID;

-- Query 8.1: สถานะ 'Pending' = รออนุมัติ
INSERT INTO CUSTOMER_REQUIREMENT (ProjectID, Title, Detail, Priority, DueDate,
                                  RelatedFeature, Status, CreatedByID, CreatedDate)
VALUES (@ProjectID, @Title, @Detail, @Priority, @DueDate,
        @RelatedFeature, 'Pending', @LoginID, GETDATE());
SET @RequirementID = SCOPE_IDENTITY();

-- Query 8.2: ทำซ้ำทุกไฟล์ (ถ้ามี)
INSERT INTO REQUIREMENT_ATTACHMENT (RequirementID, AttachmentFile)
VALUES (@RequirementID, @AttachmentFile);

-- Query 9.1
UPDATE PROJECT
SET Status = 'WaitingForApprove'
WHERE ProjectID = @ProjectID;

-- Query 10.1: แจ้ง Sale in Charge (อีเมลส่งจากแอป)
INSERT INTO NOTIFICATION (RecipientID, ProjectID, Type, Message, SendAt, IsRead)
SELECT p.SaleID, p.ProjectID, 'RequirementPending',
       CONCAT(N'มี Requirement ใหม่รออนุมัติ: ', @Title), GETDATE(), 0
FROM PROJECT p
WHERE p.ProjectID = @ProjectID;

-- ============================================================
-- UC07 Approve New Project Requirement  (Actor: Sale)
-- ============================================================
-- Query 1.1
SELECT p.ProjectID, p.ProjectName, c.CustomerName,
       COUNT(r.RequirementID) AS PendingCount
FROM PROJECT p
JOIN CUSTOMER c             ON c.CustomerID = p.CustomerID
JOIN CUSTOMER_REQUIREMENT r ON r.ProjectID  = p.ProjectID AND r.Status = 'Pending'
WHERE p.SaleID = @LoginID AND p.Status = 'WaitingForApprove'
GROUP BY p.ProjectID, p.ProjectName, c.CustomerName;

-- Query 3.1
SELECT r.RequirementID, r.Title, r.Priority, r.DueDate,
       CONCAT(e.FirstName, ' ', e.LastName) AS CreatedBy
FROM CUSTOMER_REQUIREMENT r
JOIN EMPLOYEE e ON e.EmployeeID = r.CreatedByID
WHERE r.ProjectID = @ProjectID AND r.Status = 'Pending'
ORDER BY r.CreatedDate;

-- Query 5.1: ไม่ใช่ 'Pending' → Alternative 5a
SELECT Status
FROM CUSTOMER_REQUIREMENT
WHERE RequirementID = @RequirementID;

-- Query 6.1
SELECT Title, Detail, Priority, DueDate, RelatedFeature, CreatedDate
FROM CUSTOMER_REQUIREMENT
WHERE RequirementID = @RequirementID;

-- Query 6.2
SELECT AttachmentFile
FROM REQUIREMENT_ATTACHMENT
WHERE RequirementID = @RequirementID;

-- Query 9.1: @Decision = 'Approved' / 'Rejected'
UPDATE CUSTOMER_REQUIREMENT
SET Status = @Decision, ReviewedByID = @LoginID,
    ReviewedDate = GETDATE(), RejectReason = @RejectReason
WHERE RequirementID = @RequirementID AND Status = 'Pending';

-- Query 10.1: แจ้ง PM ผู้บันทึก Requirement
INSERT INTO NOTIFICATION (RecipientID, ProjectID, Type, Message, SendAt, IsRead)
SELECT r.CreatedByID, r.ProjectID, 'RequirementResult',
       CONCAT(N'Requirement "', r.Title, N'" : ', r.Status,
              CASE WHEN r.RejectReason IS NULL THEN N''
                   ELSE CONCAT(N' (เหตุผล: ', r.RejectReason, N')') END), GETDATE(), 0
FROM CUSTOMER_REQUIREMENT r
WHERE r.RequirementID = @RequirementID;

-- Query 11.1: ยังเหลือ > 0 → Alternative 11a
SELECT COUNT(*) AS PendingLeft
FROM CUSTOMER_REQUIREMENT
WHERE ProjectID = @ProjectID AND Status = 'Pending';

-- Query 11.2: นับเฉพาะรอบนี้ = Requirement ที่บันทึกหลังนัดทดสอบล่าสุด
SELECT COUNT(*) AS ApprovedThisRound
FROM CUSTOMER_REQUIREMENT
WHERE ProjectID = @ProjectID AND Status = 'Approved'
  AND CreatedDate >= (SELECT MAX(CreatedDate) FROM TEST_SCHEDULE
                      WHERE ProjectID = @ProjectID);

-- Query 11.3: Step 11.1 (ApprovedThisRound > 0)
UPDATE PROJECT
SET Status = 'NewProject'
WHERE ProjectID = @ProjectID;

INSERT INTO NOTIFICATION (RecipientID, ProjectID, Type, Message, SendAt, IsRead)
SELECT PresaleID, ProjectID, 'RequirementResult',
       N'มี Requirement ใหม่ที่อนุมัติแล้ว กรุณาปรับ Solution', GETDATE(), 0
FROM PROJECT
WHERE ProjectID = @ProjectID;

-- Query 11.4: Step 11.2 (ApprovedThisRound = 0)
UPDATE PROJECT
SET Status = 'Finish'
WHERE ProjectID = @ProjectID;

-- ============================================================
-- UC08 Close Project  (Actor: PM)
-- ============================================================
-- Query 1.1
SELECT p.ProjectID, p.ProjectName, c.CustomerName
FROM PROJECT p
JOIN CUSTOMER c ON c.CustomerID = p.CustomerID
WHERE p.PMID = @LoginID AND p.Status = 'Testing'
ORDER BY p.ProjectID DESC;

-- Query 3.1: Step 3.1 – 3.2 ข้อมูล Project + Sale / Presale
SELECT p.ProjectID, p.ProjectName, c.CustomerName,
       CONCAT(s.FirstName, ' ', s.LastName)   AS SaleInCharge,
       CONCAT(ps.FirstName, ' ', ps.LastName) AS PresaleInCharge
FROM PROJECT p
JOIN CUSTOMER c  ON c.CustomerID  = p.CustomerID
JOIN EMPLOYEE s  ON s.EmployeeID  = p.SaleID
JOIN EMPLOYEE ps ON ps.EmployeeID = p.PresaleID
WHERE p.ProjectID = @ProjectID AND p.PMID = @LoginID;

-- Query 3.2: Step 3.2 Tech Engineer ที่ถูกมอบหมาย
SELECT e.FirstName, e.LastName
FROM PROJECT_ASSIGNMENT pa
JOIN EMPLOYEE e ON e.EmployeeID = pa.EngineerID
WHERE pa.ProjectID = @ProjectID;

-- Query 3.3: Step 3.3 นัดทดสอบล่าสุด
SELECT TOP 1 TestDate, StartTime, EndTime, Location
FROM TEST_SCHEDULE
WHERE ProjectID = @ProjectID AND Status = 'Scheduled'
ORDER BY TestDate DESC;

-- Query 5.1: ต้องเป็น 'Testing' ไม่ใช่ → Alternative 5a
SELECT Status
FROM PROJECT
WHERE ProjectID = @ProjectID AND PMID = @LoginID;

-- Query 5.2: ต้องเป็น 0 มากกว่า 0 → Alternative 5a
SELECT COUNT(*) AS PendingCount
FROM CUSTOMER_REQUIREMENT
WHERE ProjectID = @ProjectID AND Status = 'Pending';

-- Query 6.1
UPDATE PROJECT
SET Status = 'Finish'
WHERE ProjectID = @ProjectID AND Status = 'Testing';

-- Query 7.1
UPDATE TEST_SCHEDULE
SET Status = 'Done'
WHERE ProjectID = @ProjectID AND Status = 'Scheduled';

-- Query 8.1: แจ้ง Sale, Presale และ Tech Engineer ทุกคนใน Project
INSERT INTO NOTIFICATION (RecipientID, ProjectID, Type, Message, SendAt, IsRead)
SELECT r.EmployeeID, p.ProjectID, 'ProjectClosed',
       CONCAT(N'Project ปิดแล้ว: ', p.ProjectName, N' (เตรียมจัดทำเอกสาร UAT และ Handover)'), GETDATE(), 0
FROM (SELECT SaleID AS EmployeeID FROM PROJECT WHERE ProjectID = @ProjectID
      UNION
      SELECT PresaleID FROM PROJECT WHERE ProjectID = @ProjectID
      UNION
      SELECT EngineerID FROM PROJECT_ASSIGNMENT WHERE ProjectID = @ProjectID) r
CROSS JOIN PROJECT p
WHERE p.ProjectID = @ProjectID;

-- ============================================================
-- Features outside the use cases (no narrative / sequence pages; see CLAUDE.md §6.0)
-- ============================================================
-- ============================================================
-- F1 Register  (ผู้ใช้ใหม่ (เลือก Role ได้ 1 Role ต่อ 1 บัญชี))
-- ============================================================
-- Query R.1: Skill ที่มีในระบบ ใช้เป็นคำแนะนำขณะ Tech Engineer พิมพ์ Skill เอง
SELECT SkillID, SkillName
FROM SKILL
ORDER BY SkillName;

-- Query R.2: Username / Email ต้องไม่ซ้ำ (มากกว่า 0 = ซ้ำ)
SELECT COUNT(*) AS Taken
FROM EMPLOYEE
WHERE Username = @Username OR Email = @Email;

-- Query R.3: 1 บัญชี = 1 Role (Sale / Presale / PM / TechEngineer), Password เก็บเป็น BCrypt hash
INSERT INTO EMPLOYEE (FirstName, LastName, Email, Phone, Username, Password, Role)
VALUES (@FirstName, @LastName, @Email, @Phone, @Username, @PasswordHash, @Role);
SET @EmployeeID = SCOPE_IDENTITY();

-- Query R.4: Tech Engineer เท่านั้น ทำซ้ำทุก Skill ที่พิมพ์ (อย่างน้อย 1), @SkillID จาก R.5 หรือ R.6
INSERT INTO ENGINEER_SKILL (EmployeeID, SkillID)
VALUES (@EmployeeID, @SkillID);

-- Query R.5: Skill ที่พิมพ์มีในระบบแล้วหรือไม่ (ไม่สนตัวพิมพ์เล็ก / ใหญ่) ไม่พบ → R.6
SELECT SkillID
FROM SKILL
WHERE UPPER(SkillName) = UPPER(@SkillName);

-- Query R.6: เพิ่ม Skill ใหม่ที่ Tech Engineer พิมพ์เอง (จะแสดงใน Filter ของ UC04 ด้วย)
INSERT INTO SKILL (SkillName)
VALUES (@SkillName);
SET @SkillID = SCOPE_IDENTITY();

-- ============================================================
-- F2 Login  (ผู้ใช้ทุกคน (ไม่ต้องเลือก Role ตอน Login: Role มาจากบัญชี))
-- ============================================================
-- Query L.1: ตรวจ Password กับ BCrypt hash ในโปรแกรม แล้วเก็บ EmployeeID + Role ใน Session
SELECT EmployeeID, Password, Role, FirstName, LastName, ProfilePicture
FROM EMPLOYEE
WHERE Username = @Username;

-- ============================================================
-- F3 Profile  (ผู้ใช้ทุกคน (แก้ข้อมูลตัวเองได้ แต่แก้ Role / Username ไม่ได้))
-- ============================================================
-- Query P.1
SELECT EmployeeID, FirstName, LastName, Email, Phone, Username, Role, ProfilePicture
FROM EMPLOYEE
WHERE EmployeeID = @LoginID;

-- Query P.2: Tech Engineer: Skill ปัจจุบัน
SELECT s.SkillID, s.SkillName
FROM ENGINEER_SKILL es
JOIN SKILL s ON s.SkillID = es.SkillID
WHERE es.EmployeeID = @LoginID
ORDER BY s.SkillName;

-- Query P.3: Email ใหม่ต้องไม่ซ้ำกับคนอื่น (มากกว่า 0 = ซ้ำ)
SELECT COUNT(*) AS Taken
FROM EMPLOYEE
WHERE Email = @Email AND EmployeeID <> @LoginID;

-- Query P.4: แก้ข้อมูลส่วนตัว
UPDATE EMPLOYEE
SET FirstName = @FirstName, LastName = @LastName, Email = @Email, Phone = @Phone
WHERE EmployeeID = @LoginID;

-- Query P.5: รูปโปรไฟล์ JPG / PNG ไม่เกิน 5 MB เก็บไฟล์ใน ./uploads/profile/
UPDATE EMPLOYEE
SET ProfilePicture = @ProfilePicture
WHERE EmployeeID = @LoginID;

-- Query P.6: เปลี่ยนรหัสผ่าน (ตรวจรหัสเดิมด้วย L.1 ก่อน)
UPDATE EMPLOYEE
SET Password = @PasswordHash
WHERE EmployeeID = @LoginID;

-- Query P.7: Tech Engineer แก้ Skill: ลบชุดเดิม แล้วเพิ่มใหม่ด้วย R.5 / R.6 + R.4 (ใน Transaction เดียว, อย่างน้อย 1 Skill)
DELETE FROM ENGINEER_SKILL
WHERE EmployeeID = @LoginID;

-- ============================================================
-- F4 Notification Bell  (ผู้ใช้ทุกคน (กระดิ่งมุมขวาบน))
-- ============================================================
-- Query N.1: ตัวเลขบนกระดิ่ง (เฉพาะที่ถึงเวลาส่งแล้ว)
SELECT COUNT(*) AS Unread
FROM NOTIFICATION
WHERE RecipientID = @LoginID AND IsRead = 0 AND SendAt <= GETDATE();

-- Query N.2: รายการล่าสุด 20 รายการ
SELECT TOP 20 n.NotificationID, n.ProjectID, p.ProjectName, n.Type, n.Message, n.SendAt, n.IsRead
FROM NOTIFICATION n
JOIN PROJECT p ON p.ProjectID = n.ProjectID
WHERE n.RecipientID = @LoginID AND n.SendAt <= GETDATE()
ORDER BY n.SendAt DESC, n.NotificationID DESC;

-- Query N.3: คลิกรายการ → อ่านแล้ว แล้วเปิด UC03 ของ Project นั้น
UPDATE NOTIFICATION
SET IsRead = 1
WHERE NotificationID = @NotificationID AND RecipientID = @LoginID;

-- Query N.4: อ่านทั้งหมด
UPDATE NOTIFICATION
SET IsRead = 1
WHERE RecipientID = @LoginID AND IsRead = 0 AND SendAt <= GETDATE();

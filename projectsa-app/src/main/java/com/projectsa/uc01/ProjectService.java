package com.projectsa.uc01;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectsa.common.ApiException;
import com.projectsa.common.Auth;
import com.projectsa.common.SessionUser;
import com.projectsa.common.Sql;

/** UC01 Create Project Profile – runs Query 2.1–2.3, 5.1, 5.2, 7.1 of ProjectSA_SQL.sql. */
@Service
public class ProjectService {

    private final Sql sql;

    public ProjectService(Sql sql) {
        this.sql = sql;
    }

    /** Step 2: the form data (Sale in Charge = the logged-in Sale, existing customers, Presale engineers). */
    public Map<String, Object> createForm(SessionUser u) {
        Map<String, Object> p = Auth.params(u);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sale", sql.row("UC01", "2.1", p));                    // 2.1
        out.put("customers", sql.rows("UC01", "2.2", p));              // 2.2
        out.put("presales", sql.rows("UC01", "2.3", p));               // 2.3
        return out;
    }

    /** Steps 4–7. */
    @Transactional
    public long create(SessionUser u, ProjectDetectBlank.ProjectForm f) {
        ApiException.throwIfAny(ProjectDetectBlank.check(f));          // Step 4 (4a: errors per field)
        Map<String, Object> p = Auth.params(u);
        List<Map<String, Object>> presales = sql.rows("UC01", "2.3", p);
        if (presales.stream().noneMatch(r -> ((Number) r.get("EmployeeID")).longValue() == f.presaleId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ข้อมูลไม่ครบหรือไม่ถูกต้อง",
                    Map.of("presaleId", "กรุณาเลือก Presale Engineer in Charge"));
        }
        if (f.newCustomer()) {
            p.put("CustomerName", f.newCustomerName().strip());
            p.put("CustomerEmail", f.newCustomerEmail().strip());
            p.put("CustomerID", sql.insert("UC01", "5.1", p));         // 5.1 (new customer only)
        } else {
            boolean known = sql.rows("UC01", "2.2", p).stream()
                    .anyMatch(r -> ((Number) r.get("CustomerID")).longValue() == f.customerId());
            if (!known) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ข้อมูลไม่ครบหรือไม่ถูกต้อง",
                        Map.of("customerId", "ไม่พบลูกค้าที่เลือก"));
            }
            p.put("CustomerID", f.customerId());
        }
        p.put("ProjectName", f.projectName().strip());
        p.put("ProjectSite", f.projectSite().strip());
        p.put("PresaleID", f.presaleId());
        long projectId = sql.insert("UC01", "5.2", p);                 // 5.2 (+ Step 6: Status 'NewProject')
        p.put("ProjectID", projectId);
        sql.update("UC01", "7.1", p);                                  // 7.1 notify Presale in Charge
        return projectId;
    }
}

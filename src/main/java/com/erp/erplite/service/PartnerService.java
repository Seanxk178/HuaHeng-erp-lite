package com.erp.erplite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.entity.Partner;
import com.erp.erplite.mapper.PartnerMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerMapper partnerMapper;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            // Check and add 'address' and 'main_product' columns to base_partner
            jdbcTemplate.execute("ALTER TABLE base_partner ADD COLUMN address VARCHAR(255) NULL");
            log.info("Added column 'address' to base_partner.");
        } catch (Exception e) {
            // Column might already exist, ignore
        }
        try {
            jdbcTemplate.execute("ALTER TABLE base_partner ADD COLUMN main_product VARCHAR(255) NULL");
            log.info("Added column 'main_product' to base_partner.");
        } catch (Exception e) {
            // Column might already exist, ignore
        }
    }

    /**
     * 查询所有往来单位列表
     */
    public List<Partner> getAllPartners() {
        log.info("查询往来单位列表");
        return partnerMapper.selectList(null);
    }

    /**
     * 新增往来单位
     */
    public void addPartner(Partner partner) {
        log.info("准备新增往来单位: {}", partner.getName());

        if (partner.getCode() == null || partner.getName() == null || partner.getType() == null) {
            throw new RuntimeException("编码、名称和类型不能为空");


        }

        // 校验编码是否已存在 (防重复录入)
        QueryWrapper<Partner> query = new QueryWrapper<>();
        query.eq("code", partner.getCode());
        if (partnerMapper.exists(query)) {
            throw new RuntimeException("往来单位编码已存在，请更换！");
        }

        partnerMapper.insert(partner);
        log.info("往来单位新增成功, ID: {}", partner.getId());
    }

    /**
     * 更新往来单位
     */
    public void updatePartner(Partner partner) {
        if (partner.getId() == null) throw new RuntimeException("单位ID不能为空");
        QueryWrapper<Partner> query = new QueryWrapper<>();
        query.eq("code", partner.getCode()).ne("id", partner.getId());
        if (partnerMapper.exists(query)) {
            throw new RuntimeException("往来单位编码已存在，请更换！");
        }
        partnerMapper.updateById(partner);
    }

    /**
     * 批量导入往来单位
     */
    public String batchAddPartners(List<com.erp.erplite.dto.PartnerExcelDTO> list) {
        int successCount = 0;
        int skipCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            com.erp.erplite.dto.PartnerExcelDTO dto = list.get(i);
            int rowNum = i + 2; // +1 for 0-index, +1 for header

            // trim whitespace
            if (dto.getCode() != null) dto.setCode(dto.getCode().trim());
            if (dto.getName() != null) dto.setName(dto.getName().trim());
            if (dto.getTypeStr() != null) dto.setTypeStr(dto.getTypeStr().trim());

            if (dto.getCode() == null || dto.getCode().isEmpty() || dto.getName() == null || dto.getName().isEmpty()) {
                errors.add("第" + rowNum + "行：编码或名称为空");
                continue;
            }

            Integer type = null;
            if ("供应商".equals(dto.getTypeStr())) {
                type = 1;
            } else if ("客户".equals(dto.getTypeStr())) {
                type = 2;
            } else {
                errors.add("第" + rowNum + "行：类型错误（必须填写 供应商 或 客户）");
                continue;
            }

            QueryWrapper<Partner> query = new QueryWrapper<>();
            query.eq("code", dto.getCode());
            if (partnerMapper.exists(query)) {
                skipCount++;
                errors.add("第" + rowNum + "行：编码重复，已跳过 (" + dto.getCode() + ")");
                continue;
            }

            Partner p = new Partner();
            p.setCode(dto.getCode());
            p.setName(dto.getName());
            p.setType(type);
            p.setContact(dto.getContact());
            p.setPhone(dto.getPhone());
            p.setAddress(dto.getAddress());
            p.setMainProduct(dto.getMainProduct());
            p.setStatus(1); // 默认启用
            p.setCreateTime(new java.util.Date());
            
            partnerMapper.insert(p);
            successCount++;
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("导入完成: 成功 " + successCount + " 条，异常 " + errors.size() + " 条。详细原因：\n" + String.join("\n", errors));
        }

        return "导入成功: 共导入 " + successCount + " 条记录，跳过 " + skipCount + " 条重复记录。";
    }
}
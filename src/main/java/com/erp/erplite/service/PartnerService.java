package com.erp.erplite.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.erp.erplite.entity.Partner;
import com.erp.erplite.mapper.PartnerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerMapper partnerMapper;

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
}
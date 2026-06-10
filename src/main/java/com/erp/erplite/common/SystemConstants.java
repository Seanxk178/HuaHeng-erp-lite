package com.erp.erplite.common;

/**
 * 系统通用常量定义
 * 用于消除散落在代码中的魔法值 (Magic Strings)
 */
public interface SystemConstants {

    // 角色常量
    String ROLE_ADMIN = "admin";
    String ROLE_SALES = "sales";
    String ROLE_WAREHOUSE = "warehouse";
    String ROLE_PURCHASE = "purchase";
    String ROLE_FINANCE = "finance";
    String ROLE_PURCHASE_MANAGER = "purchase_manager";
    String ROLE_SALES_MANAGER = "sales_manager";

    // 历史数据的 MD5 盐值 (仅用于平滑升级过渡)
    String OLD_MD5_SALT = "ERP_LITE_SALT";

}

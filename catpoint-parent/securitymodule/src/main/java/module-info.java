module securitymodule{
    exports security.data;
    exports security.model;
    exports security.service;
    requires com.google.common;
    requires com.google.gson;
    requires java.desktop;
    requires imagemodule;
    opens security.model to com.google.gson;
    opens security.data to com.google.gson;
}
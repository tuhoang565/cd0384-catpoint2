module catpointmodule {
    requires java.desktop;
    requires securitymodule;
    requires java.prefs;
    requires com.google.gson;
    requires imagemodule;
    requires miglayout;
    opens catpoint.data to com.google.gson;
}
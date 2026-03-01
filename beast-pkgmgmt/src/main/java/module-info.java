open module beast.pkgmgmt {
    requires java.xml;
    requires java.desktop;
    requires static org.apache.maven.resolver;
    requires static org.apache.maven.resolver.supplier;
    requires static org.apache.maven.resolver.util;
    requires static org.slf4j;

    exports beast.pkgmgmt;
    exports beast.pkgmgmt.launcher;
}

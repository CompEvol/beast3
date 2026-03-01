open module beast.pkgmgmt {
    requires java.xml;
    requires java.desktop;
    requires org.apache.maven.resolver;
    requires org.apache.maven.resolver.supplier;
    requires org.apache.maven.resolver.util;
    requires org.slf4j;

    exports beast.pkgmgmt;
    exports beast.pkgmgmt.launcher;
}

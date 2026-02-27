package beastfx.app.tools;

import javax.swing.ImageIcon;

/**
     * package application information required for launching the app and
     * displaying in list box
     **/
    class PackageApp {
        String packageName;
        String jarDir;
        String description;
        String className;
        String argumentsString;
        ImageIcon icon;

        public String[] getArgs() {
            if (argumentsString == null || argumentsString.trim().isEmpty()) {
                return new String[]{};
            } else {
                String[] args = argumentsString.split(" ", -1);
//                System.out.println("package = " + packageName + ", class = " + className + ", args = " + Arrays.toString(args));
                return args;
            }
        }

        @Override
        public String toString() {
            return description;
        }
    }
# terminal-app

A simple terminal file manager inspired by ranger.
It is work in progress and many essential features like opening a file are not implemented yet.

## Concept

There are two different modes, Preview-mode and Split-mode.
Preview-mode is like ranger, there are 3 collumns, the first one shows the parent directory, the middle one the current directory where you can move between the files, and the right one shows a preview of the selected element. If a directory is selected, the files inside will be displayed and if a text-file is selected the content of it will be displayed.

In Spit-mode, the preview will be replaced with another directory. This allows the user to easily copy files from the main directory to the second one on the right side.

## Usage

Running the file-manager will always open the home directory, however passing in $PWD as first argument will run it at the current working directory.

### Navigation

Similar to ranger, the navigation is done using vim-like keybindings by default. All usable keys can be displayed by pressing '?'. The basic movements are done using either the arrow-keys or hjkl.

### Features

At this point, only a few arbitrarily selected features that seemed interesting to implement are present.
This also means some essential features like opening files, deleting files or selecting multiple files are not there yet.

#### Search

Press '/' and insert the search query. This will color all matching files yellow and select the next result to the curser. 
After pressing the enter-key, 'N' and 'n' can be used to jump up or down between the results.

### Copy files

In order to copy a file to another directory, the Split-mode can be used:
Navigate to the path you want to copy to. Then press 'm' to activate Split-mode, which will show the directory at the right side as well. Now move to the file you want to copy and press '>' to copy it over.

### Configuration

TODO

## License

Copyright © 2021 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

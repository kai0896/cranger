# Cranger file-manager

A simple terminal file manager inspired by ranger written in clojure.
It is work in progress and many essential features are not implemented yet.
This was created as part of an university assignment about functional programming with clojure.

## Concept

There are two different modes, Preview-mode and Split-mode.
Preview-mode is like ranger, there are 3 collumns, the first one shows the parent directory, the middle one the current directory where you can move between the files, and the right one shows a preview of the selected element. If a directory is selected, the files inside will be displayed and if a text-file is selected the content of it will be displayed.

In Spit-mode, the preview will be replaced with another directory. This allows the user to easily copy files from the main directory to the second one on the right side.

## Usage

Cd into the repo and use leiningen to run or to build a jar.
``` sh
lein run
```

Running the file-manager will always open the home directory, however passing in $PWD as first argument will run it at the current working directory.

### Navigation

Similar to ranger, the navigation is done using vim-like keybindings by default. All usable keys can be displayed by pressing '?'. The basic movements are done using either the arrow-keys or hjkl.

### Features

At this point, only a few arbitrarily selected features that seemed interesting to implement are present.
This also means some essential features like opening files, deleting files or selecting multiple files are not there yet.

**Search:** Press '/' and insert the search query. This will color all matching files yellow and select the next result to the curser. 
After pressing the enter-key, 'N' and 'n' can be used to jump up or down between the results.

**Copy files:** In order to copy a file to another directory, the Split-mode can be used:
Navigate to the path you want to copy to. Then press 'm' to activate Split-mode, which will show the directory at the right side as well. Now move to the file you want to copy and press '>' to copy it over.

**Configuration:** Using configuration-files, keybindings and colors can be changed. Config-files have to be either '~/.config/cranger/config.edn' or '~/.cranger/config.edn'. Here is an example:

``` clojure
{:keybinds {\w :sel-up
            \a :folder-up
            \s :sel-down
            \d :folder-down}
   :colors {:primary :red}}
```
This will add wasd as navigation keys and change the primary color to red.
(refer to config.clj to see the default configuration)

## Dependencies

Clojure-lanterna: https://multimud.github.io/clojure-lanterna/  
This libary is used to draw text to different positions of the terminal window, as well as to read keyboard inputs.

Raynes fs: https://github.com/Raynes/fs  
Some helpers to work with files. This is currently only used for copying files and directories.

## Code Structure

All state is stored in a map which is initialized at the start given a starting file-path and configuration.
Then the programm runs in a loop (core.clj), where it renders (render.clj) based on the state and waits for an input. Depending on the key pressed, a function is called which takes in the state and returns the updated state (navigate.clj). Now the loop starts again, rendering based on the new state.

## License

Copyright <2022> <Kai Hähnle>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

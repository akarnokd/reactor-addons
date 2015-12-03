/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var gulp = require('gulp'),
  concat = require('gulp-concat');

module.exports = function(config){
  return function(){

    var p = gulp.src(config.classpathSource+'assets/js/pylon.js')
      .pipe(gulp.dest(config.classpathTarget+'assets/js'));

    if(config.classpathDevTarget !== 'undefined'){
      p.pipe(gulp.dest(config.classpathDevTarget));
    }

    if(config.vendor.js.length === 0) return;

    p = gulp.src(config.vendor.js)
      .pipe(concat('vendor.js'))
      .pipe(gulp.dest(config.classpathTarget+'assets/js'));

    if(config.classpathDevTarget !== 'undefined'){
      p.pipe(gulp.dest(config.classpathDevTarget));
    }

    p = gulp.src(config.vendor.map)
      .pipe(gulp.dest(config.classpathTarget+'assets/js'));

    if(config.classpathDevTarget !== 'undefined'){
      p = p.pipe(gulp.dest(config.classpathDevTarget));
    }

    return p;

  };
};
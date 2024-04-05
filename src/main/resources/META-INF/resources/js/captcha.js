/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

window.addEventListener('load', () => {


    let captchaInput = document.getElementById('captcha-input');
    let captchaImage = document.getElementById('captcha-image');
    let captchaRefresh = document.getElementById('captcha-refresh');
    let captchaPlay = document.getElementById('captcha-play');
    let captchaStop = document.getElementById('captcha-stop');

    // check if error=capcha is in the URL
    let url = new URL(window.location.href);
    let error = url.searchParams.get('error');
    if (error === 'captcha') {
        captchaInput.classList.add('is-invalid');
    }

    captchaInput.addEventListener('input', function() {
        captchaInput.classList.remove('is-invalid');
    });

    captchaRefresh.addEventListener('click', function(e) {
        e.preventDefault();
        captchaImage.src = captchaImage.src+'&rng='+Math.random();
        audio = new Audio(window.webApplicationBaseURL + '/servlets/MIRMailerWithFile?action=captcha-play&rng='+Math.random());
        audio.addEventListener('ended', onEnd);
    });

    let audio = new Audio(window.webApplicationBaseURL + '/servlets/MIRMailerWithFile?action=captcha-play');

    audio.preload = 'auto';

    captchaPlay.addEventListener('click', function(e) {
        e.preventDefault()
        audio.play();
        captchaPlay.classList.add('d-none');
        captchaStop.classList.remove('d-none');
    });

    const onEnd = function(e) {
        e.preventDefault()
        audio.pause();
        audio.currentTime= 0;
        captchaPlay.classList.remove('d-none');
        captchaStop.classList.add('d-none');
    };

    captchaStop.addEventListener('click', onEnd);
    audio.addEventListener('ended', onEnd);

});
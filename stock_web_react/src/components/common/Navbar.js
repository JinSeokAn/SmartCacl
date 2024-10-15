import React from 'react';
import './Header.css';

const Header = () => {
    return (
        <header className="header">
            <h1>My Application</h1>
            <nav>
                <ul>
                    <li><a href="/">Home</a></li>
                    <li><a href="/about">About</a></li>
                </ul>
            </nav>
        </header>
    );
};

export default Header;
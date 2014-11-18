import os
from setuptools import setup

# allow setup.py to be run from any path
os.chdir(os.path.normpath(os.path.join(os.path.abspath(__file__), os.pardir)))

setup(
    name='refine_viaf',
    version='0.1',
    packages=['refine_viaf'],
    install_requires=['beautifulsoup4', 'requests'],
    include_package_data=True,
    license='BSD License',
    description='VIAF Reconciliation Service for OpenRefine',
    long_description='refine_viaf implements an OpenRefine Reconciliation Service that queries the VIAF public API. Works with Django and other web frameworks. See the project page on GitHub for more details.',
    url='http://www.example.com/',
    author='Jeff Chiu',
    author_email='jeff@codefork.com',
    classifiers=[
        'Development Status :: 4 - Beta',
        'Environment :: Web Environment',
        'Framework :: Django',
        'Intended Audience :: Developers',
        'Intended Audience :: Information Technology',
        'License :: OSI Approved :: BSD License',
        'Programming Language :: Python',
        'Topic :: Internet :: WWW/HTTP',
    ],
)
